/**
 * MotelsWorkerRunnable
 *
 * Copyright (C) 2012 Sh1fT
 *
 * This file is part of Serveur_Occupations.
 *
 * Serveur_Occupations is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * Serveur_Occupations is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Serveur_Occupations; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 */

package serveur_occupations;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.xml.sax.InputSource;
import protocols.ProtocolRLP;

/**
 * Manage a {@link MotelsWorkerRunnable}
 * @author Sh1fT
 */
public class MotelsWorkerRunnable implements Runnable {
    private Serveur_Occupations parent;
    private Socket cSocket;

    /**
     * Create a new {@link MotelsWorkerRunnable} instance
     * @param parent
     * @param cSocket 
     */
    public MotelsWorkerRunnable(Serveur_Occupations parent, Socket cSocket) {
        this.setParent(parent);
        this.setcSocket(cSocket);
    }

    public Serveur_Occupations getParent() {
        return parent;
    }

    public void setParent(Serveur_Occupations parent) {
        this.parent = parent;
    }

    public Socket getcSocket() {
        return cSocket;
    }

    public void setcSocket(Socket cSocket) {
        this.cSocket = cSocket;
    }

    public void run() {
        try {
            this.getParent().getClientLabel().setText(
                    this.getcSocket().getInetAddress().getHostAddress());
            InputSource is = new InputSource(new InputStreamReader(
                    this.getcSocket().getInputStream()));
            BufferedReader br = new BufferedReader(is.getCharacterStream());
            ObjectOutputStream oos = new ObjectOutputStream(
                    this.getcSocket().getOutputStream());
            String cmd = br.readLine();
            if (cmd.contains("EXCHKEY")) {
                 this.getParent().setClientPublicKey(
                        Base64.decode(cmd.split(":")[1]));
                oos.writeObject(this.getParent().getKeyPair().getPublic());
            } else if (cmd.contains("SENDKEY")) {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
                cipher.init(Cipher.DECRYPT_MODE,
                        this.getParent().getKeyPair().getPrivate());
                this.getParent().setSecretKey(new SecretKeySpec(cipher.doFinal(
                        Base64.decode(cmd.split(":")[1])), "AES"));
                oos.writeObject("OK");
            } else if (cmd.contains("LOGIN")) {
                String username = cmd.split(":")[1];
                String password = cmd.split(":")[2];
                Integer res = this.getParent().getProtocolRLP().
                        login(username, password);
                switch (res) {
                    case ProtocolRLP.RESPONSE_OK:
                        oos.writeObject("OK");
                        break;
                    case ProtocolRLP.RESPONSE_KO:
                        oos.writeObject("KO");
                        break;
                    default:
                        oos.writeObject("KO");
                        break;
                }
            } else if (cmd.contains("BDROOM")) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
                Date arrival = new Date(sdf.parse(cmd.split(":")[1]).getTime());
                String signature = cmd.split(":")[2];
                String res = null;
                if (this.getParent().verifySignature(signature, "BDROOM_SIG",
                        this.getParent().getClientPublicKey()))
                    res = this.getParent().getProtocolRLP().bookedRoom(arrival);
                else
                    res = "KO";
                oos.writeObject(this.getParent().encryptClientName(res));
            } else if (cmd.contains("ARRROOM")) {
                String idRoom = cmd.split(":")[1];
                String clientName = this.getParent().decryptClientName(
                        cmd.split(":")[2]);
                String signature = cmd.split(":")[3];
                Integer res = null;
                if (this.getParent().verifySignature(signature, "ARRROOM_SIG",
                        this.getParent().getClientPublicKey()))
                    res = this.getParent().getProtocolRLP().
                            arrivalRoom(idRoom, clientName);
                switch (res) {
                    case ProtocolRLP.RESPONSE_OK:
                        oos.writeObject("OK");
                        break;
                    case ProtocolRLP.RESPONSE_KO:
                        oos.writeObject("KO");
                        break;
                    default:
                        oos.writeObject("KO");
                        break;
                }
            } else if (cmd.contains("MISROOM")) {
                String idRoom = cmd.split(":")[1];
                String clientName = this.getParent().decryptClientName(
                        cmd.split(":")[2]);
                String signature = cmd.split(":")[3];
                Integer res = null;
                if (this.getParent().verifySignature(signature, "MISROOM_SIG",
                        this.getParent().getClientPublicKey()))
                    res = this.getParent().getProtocolRLP().
                            missingRoom(idRoom, clientName);
                else
                    res = ProtocolRLP.RESPONSE_KO;
                switch (res) {
                    case ProtocolRLP.RESPONSE_OK:
                        oos.writeObject("OK");
                        break;
                    case ProtocolRLP.RESPONSE_KO:
                        oos.writeObject("KO");
                        break;
                    default:
                        oos.writeObject("KO");
                        break;
                }
            }
            oos.close();
            br.close();
            this.getcSocket().close();
            this.getParent().getClientLabel().setText("aucun");
        } catch (IOException | ParseException | NoSuchAlgorithmException |
                NoSuchProviderException | IllegalBlockSizeException |
                InvalidKeyException | NoSuchPaddingException |
                BadPaddingException ex) {
            System.out.println("Error: " + ex.getLocalizedMessage());
            System.exit(1);
        }
    }
}