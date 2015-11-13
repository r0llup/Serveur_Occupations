/**
 * ProtocolRLP
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

package protocols;

import beans.BeanDBAccessMySQL;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import serveur_occupations.Serveur_Occupations;

/**
 * Manage a {@link ProtocolRLP}
 * @author Sh1fT
 */
public class ProtocolRLP implements interfaces.ProtocolRLP {
    private Serveur_Occupations parent;
    private BeanDBAccessMySQL bdbam;
    public static final int RESPONSE_OK = 100;
    public static final int RESPONSE_KO = 600;

    /**
     * Create a new {@link ProtocolRLP} instance
     * @param parent 
     */
    public ProtocolRLP(Serveur_Occupations parent) {
        this.setParent(parent);
        this.setBdbam(new BeanDBAccessMySQL(
                System.getProperty("file.separator") +"properties" +
                System.getProperty("file.separator") + "BeanDBAccessMySQL.properties"));
    }

    public Serveur_Occupations getParent() {
        return parent;
    }

    public void setParent(Serveur_Occupations parent) {
        this.parent = parent;
    }

    public BeanDBAccessMySQL getBdbam() {
        return bdbam;
    }

    public void setBdbam(BeanDBAccessMySQL bdbam) {
        this.bdbam = bdbam;
    }

    /**
     * Effectue la connexion pour un gestionnaire de motels
     * @param name
     * @param password
     * @return 
     */
    @Override
    public Integer login(String name, String password) {
        try {
            String query = "SELECT * FROM gestionnairemotels WHERE nom LIKE ? " +
                "AND password LIKE ?;";
            PreparedStatement ps = this.getBdbam().getDBConnection().prepareStatement(query);
            ps.setString(1, name);
            ps.setString(2, password);
            ResultSet rs = this.getBdbam().executeQuery(ps);
            if (rs.next())
                return ProtocolRLP.RESPONSE_OK;
            return ProtocolRLP.RESPONSE_KO;
        } catch (SQLException ex) {
            System.out.println("Error: " + ex.getLocalizedMessage());
            this.getBdbam().stop();
            System.exit(1);
        }
        return null;
    }

    /**
     * Liste des réservations pour le motel/village pour un jour donné
     * @param arrival
     * @return 
     */
    @Override
    public String bookedRoom(Date arrival) {
        try {
            String listRooms = "";
            String query = "SELECT chambre, nom FROM reservations, voyageurs WHERE " +
                "reservations.voyageurTitulaire = voyageurs.idVoyageur AND " +
                "reservations.dateArrivee = ?;";
            PreparedStatement ps = this.getBdbam().getDBConnection().prepareStatement(query);
            ps.setDate(1, arrival);
            ResultSet rs = this.getBdbam().executeQuery(ps);
            while (rs.next())
                listRooms += rs.getString("chambre") + ":" + rs.getString("nom") + ":";
            return listRooms;
        } catch (SQLException ex) {
            System.out.println("Error: " + ex.getLocalizedMessage());
            this.getBdbam().stop();
            System.exit(1);
        }
        return null;
    }

    /**
     * Les titulaires d'une réservation sont arrivés
     * @param idRoom
     * @param clientName
     * @return 
     */
    @Override
    public Integer arrivalRoom(String idRoom, String clientName) {
        try {
            String query = "SELECT idVoyageur FROM voyageurs WHERE nom LIKE ?;";
            PreparedStatement ps = this.getBdbam().getDBConnection().prepareStatement(query);
            ps.setString(1, clientName);
            ResultSet rs = this.getBdbam().executeQuery(ps);
            Integer idVoyageur = null;
            if (rs.next()) {
                idVoyageur = rs.getInt("idVoyageur");
                query = "SELECT * FROM reservations WHERE voyageurTitulaire = ? " +
                        "AND chambre LIKE ? AND dateArrivee <= CURRENT_DATE;";
                ps = this.getBdbam().getDBConnection().prepareStatement(query);
                ps.setInt(1, idVoyageur);
                ps.setString(2, idRoom);
                rs = this.getBdbam().executeQuery(ps);
                if (rs.next())
                    return ProtocolRLP.RESPONSE_OK;
                return ProtocolRLP.RESPONSE_KO;
            }
        } catch (SQLException ex) {
            System.out.println("Error: " + ex.getLocalizedMessage());
            this.getBdbam().stop();
            System.exit(1);
        }
        return null;
    }

    /**
     * Signale que les titulaires d'une réservation ne sont pas arrivés
     * @param idRoom
     * @param clientName
     * @return 
     */
    @Override
    public Integer missingRoom(String idRoom, String clientName) {
        try {
            String query = "SELECT idVoyageur FROM voyageurs WHERE nom LIKE ?;";
            PreparedStatement ps = this.getBdbam().getDBConnection().prepareStatement(query);
            ps.setString(1, clientName);
            ResultSet rs = this.getBdbam().executeQuery(ps);
            Integer idVoyageur = null;
            if (rs.next()) {
                idVoyageur = rs.getInt("idVoyageur");
                query = "UPDATE reservations SET misroom = TRUE WHERE " +
                        "voyageurTitulaire = ? AND chambre LIKE ?;";
                ps = this.getBdbam().getDBConnection().prepareStatement(query);
                ps.setInt(1, idVoyageur);
                ps.setString(2, idRoom);
                Integer rss = this.getBdbam().executeUpdate(ps);
                if (rss == 1) {
                    this.getBdbam().getDBConnection().commit();
                    return ProtocolRLP.RESPONSE_OK;
                } else
                    return ProtocolRLP.RESPONSE_KO;
            }
        } catch (SQLException ex) {
                System.out.println("Error: " + ex.getLocalizedMessage());
                this.getBdbam().stop();
                System.exit(1);
        }
        return null;
    }
}