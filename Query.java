package flightapp;

import java.io.IOException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Query extends QueryAbstract {
  

  // Create a user 
  private static final String CREATE_USER_SQL = "INSERT INTO Users VALUES(?, ?, ?)";
  private PreparedStatement createUserStmt;

  // Fetch info about an existing user
  private static final String FETCH_USER_SQL
    = "SELECT username, hashedPassword, balance"
    + "  FROM Users WHERE username = ?";
  private PreparedStatement fetchUserStmt;

  private static final String ONE_HOP_SQL
    = "   SELECT TOP(?) fid, day_of_month, carrier_id, flight_num, origin_city, dest_city,"
    + "          actual_time, capacity, price"
    + "     FROM Flights"
    + "    WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? AND canceled != 1"
    + " ORDER BY actual_time ASC";
  private PreparedStatement oneHopStmt;
  
  // Two-hop itineraries
  private static final String TWO_HOP_SQL
    = "  SELECT "
    // First-hop fields:
    + "         TOP(?) f1.fid AS f1_fid, f1.day_of_month AS f1_day_of_month,"
    + "         f1.carrier_id AS f1_carrier_id, f1.flight_num AS f1_flight_num,"
    + "         f1.origin_city AS f1_origin_city, f1.dest_city AS f1_dest_city,"
    + "         f1.actual_time AS f1_actual_time, f1.capacity as f1_capacity,"
    + "         f1.price as f1_price,"
    // Second-hop fields:
    + "         f2.fid AS f2_fid, f2.day_of_month AS f2_day_of_month,"
    + "         f2.carrier_id AS f2_carrier_id, f2.flight_num AS f2_flight_num,"
    + "         f2.origin_city AS f2_origin_city, f2.dest_city AS f2_dest_city,"
    + "         f2.actual_time as f2_actual_time,"
    + "         (f1.actual_time + f2.actual_time) AS total_time,"
    + "         f2.capacity as f2_capacity, f2.price as f2_price"
    + "    FROM Flights f1, Flights f2 "
    + "   WHERE f1.origin_city = ? AND f2.dest_city = ? AND f1.day_of_month = ?"
    + "     AND f2.day_of_month = f1.day_of_month AND f1.dest_city = f2.origin_city"
    + "     AND f1.canceled != 1 AND f2.canceled != 1 "
    + "ORDER BY total_time, f1_fid, f2_fid ASC";
  private PreparedStatement twoHopStmt;
  
  private static final String NO_OTHER_FLIGHTS_SQL
    = "SELECT 1 FROM Reservations r, Flights f "
    + "WHERE r.fid1 = f.fid AND r.username = ? AND f.day_of_month = ?";
  private PreparedStatement noOtherFlightsStmt;

  private static final String REMAINING_CAPACITY_SQL
    = "SELECT fid, capacity - SUM(cnt) AS availability "
    + "FROM ("
    + "   SELECT f.fid AS fid, f.capacity AS capacity, COUNT(r.res_id) AS cnt"
    + "     FROM Flights f LEFT OUTER JOIN Reservations r ON f.fid = r.fid1"
    + "    WHERE f.fid = ?"
    + " GROUP BY f.fid, f.capacity "
    + " UNION ALL"
    + "   SELECT f.fid AS fid, f.capacity AS capacity, COUNT(r.res_id) AS cnt"
    + "     FROM Flights f LEFT OUTER JOIN Reservations r ON f.fid = r.fid2"
    + "    WHERE f.fid = ?"
    + " GROUP BY f.fid, f.capacity"
    + ") AS x "
    + "GROUP BY fid, capacity";
  private PreparedStatement remainingCapacityStmt;

  private static final String GET_NEXT_RESERVATION_SQL
    = "SELECT COUNT(*) AS next_id FROM Reservations";
  private PreparedStatement getNextReservationStmt;

  private static final String CREATE_ONE_FLIGHT_RESERVATION_SQL
    = "INSERT INTO Reservations VALUES (?, 0, ?, ?, null)";
  private PreparedStatement createOneFlightReservationStmt;

  private static final String CREATE_TWO_FLIGHT_RESERVATION_SQL
    = "INSERT INTO Reservations VALUES (?, 0, ?, ?, ?)";
  private PreparedStatement createTwoFlightReservationStmt;

  private static final String FIND_PAYABLE_RESERVATION_SQL
    = "SELECT f.price AS price, r.fid2 AS fid2"
    + "  FROM Reservations r, Flights f"
    + " WHERE r.res_id = ? AND r.paid = 0 AND r.username = ?"
    + "   AND r.fid1 = f.fid";
  private PreparedStatement findPayableReservationStmt;

  private static final String GET_FLIGHT_PRICE_SQL = "SELECT price FROM Flights WHERE fid = ?";
  private PreparedStatement getFlightPriceStmt;

  private static final String GET_USER_BALANCE_SQL
    = "SELECT balance FROM Users WHERE username = ?";
  private PreparedStatement getUserBalanceStmt;

  private static final String PAY_RESERVATION_SQL
    = "UPDATE Reservations SET paid=1 WHERE res_id = ?;"
    + "UPDATE Users SET balance = ? WHERE username = ?";
  private PreparedStatement payReservationStmt;

  private static final String GET_RESERVATIONS_SQL
    = "SELECT r.res_id AS res_id, r.paid AS paid, r.fid1 AS fid1, r.fid2 AS fid2,"
    + "       f.day_of_month AS day_of_month, f.carrier_id AS carrier_id,"
    + "       f.flight_num AS flight_num, f.origin_city AS origin_city, f.dest_city AS dest_city,"
    + "       f.actual_time AS actual_time, f.capacity AS capacity, f.price AS price"
    + "  FROM Reservations r, Flights f"
    + " WHERE r.username = ?"
    + "   AND r.fid1 = f.fid";
  private PreparedStatement getReservationsStmt;

  private static final String GET_FLIGHT_INFO_SQL
    = "  SELECT fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time,"
    + "         capacity, price"
    + "    FROM Flights"
    + "   WHERE fid = ?";
  private PreparedStatement getFlightInfoStmt;  
  
  
  String currUser;
  List<Itinerary> currSearch;

  
  protected Query() throws SQLException, IOException {
    prepareStatements();
  }

  public void clearTables() {
    try {
      Statement s = conn.createStatement();
      s.executeUpdate("DELETE FROM Reservations");
      s.executeUpdate("DELETE FROM Users");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  
  private void prepareStatements() throws SQLException {
    createUserStmt = conn.prepareStatement(CREATE_USER_SQL);
    fetchUserStmt = conn.prepareStatement(FETCH_USER_SQL);
    oneHopStmt = conn.prepareStatement(ONE_HOP_SQL);
    twoHopStmt = conn.prepareStatement(TWO_HOP_SQL);
    noOtherFlightsStmt = conn.prepareStatement(NO_OTHER_FLIGHTS_SQL);
    remainingCapacityStmt = conn.prepareStatement(REMAINING_CAPACITY_SQL);
    getNextReservationStmt = conn.prepareStatement(GET_NEXT_RESERVATION_SQL);
    createOneFlightReservationStmt = conn.prepareStatement(CREATE_ONE_FLIGHT_RESERVATION_SQL);
    createTwoFlightReservationStmt = conn.prepareStatement(CREATE_TWO_FLIGHT_RESERVATION_SQL);
    findPayableReservationStmt = conn.prepareStatement(FIND_PAYABLE_RESERVATION_SQL);
    getFlightPriceStmt = conn.prepareStatement(GET_FLIGHT_PRICE_SQL);
    getUserBalanceStmt = conn.prepareStatement(GET_USER_BALANCE_SQL);
    payReservationStmt = conn.prepareStatement(PAY_RESERVATION_SQL);
    getReservationsStmt = conn.prepareStatement(GET_RESERVATIONS_SQL);
    getFlightInfoStmt = conn.prepareStatement(GET_FLIGHT_INFO_SQL);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged in\n".  For all
   *         other errors, return "Login failed\n". Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password) {
    final String FAILURE = "Login failed\n";

    if (currUser != null) {
      return "User already logged in\n";
    }
    username = username.toLowerCase();

    try {
      beginTransaction();

      byte[] hashedPassword;

      fetchUserStmt.setString(1, username);

      ResultSet results = fetchUserStmt.executeQuery();
      if (!results.next()) {
        rollbackTransaction();
        return FAILURE;
      } else {
        hashedPassword = results.getBytes("hashedPassword");
      }

      if (!PasswordUtils.plaintextMatchesHash(password, hashedPassword)) {
        rollbackTransaction();
        return FAILURE;
      }

      currUser = username;
      currSearch = null;
      rollbackTransaction(); 
      return "Logged in as " + username + "\n";
    } catch (SQLException ex) {
      try {
        rollbackTransaction();
        if (isDeadlock(ex)) {
          return transaction_login(username, password);
        } else {
          return FAILURE;
        }
      } catch (SQLException innerEx) {

        innerEx.printStackTrace();
        return FAILURE;
      }
    }
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure
   *                   otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    final String FAILURE = "Failed to create user\n";

    if (initAmount < 0) {
      return FAILURE;
    }

    String response;
    try {
      beginTransaction();

      fetchUserStmt.setString(1, username);
      ResultSet results = fetchUserStmt.executeQuery();
      if (results.next()) {
        rollbackTransaction();
        return FAILURE;
      }

      byte[] hashedPassword = PasswordUtils.hashPassword(password);
      createUserStmt.setString(1, username);
      createUserStmt.setBytes(2, hashedPassword);
      createUserStmt.setInt(3, initAmount);

      int rowsAffected = createUserStmt.executeUpdate();
      if (rowsAffected != 1) {
        rollbackTransaction();
        return FAILURE;
      } else {
        commitTransaction();
        return "Created user " + username + "\n";
      }
    } catch (SQLException ex) {
      try {
        rollbackTransaction();
        if (isDeadlock(ex)) {
          return transaction_createCustomer(username, password, initAmount);
        } else {
          return FAILURE;
        }
      } catch (SQLException innerEx) {
        // Same comment about dead code
        innerEx.printStackTrace();
        return FAILURE;
      }
    }
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination city, on the given
   * day of the month. If {@code directFlight} is true, it only searches for direct flights,
   * otherwise is searches for direct flights and flights with two "hops." Only searches for up
   * to the number of itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights, otherwise include
   *                            indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return, must be positive
   *
   * @return If no itineraries were found, return "No flights match your selection\n". If an error
   *         occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total flight time]
   *         minutes\n [first flight in itinerary]\n ... [last flight in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *         Itinerary numbers in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    final String FAILURE = "Failed to search\n";

    if (numberOfItineraries <= 0) {
      return FAILURE;
    }

    currSearch = new ArrayList<>();
    try {
      oneHopStmt.setInt(1, numberOfItineraries);
      oneHopStmt.setString(2, originCity);
      oneHopStmt.setString(3, destinationCity);
      oneHopStmt.setInt(4, dayOfMonth);

      ResultSet results = oneHopStmt.executeQuery();
      while (results.next() && currSearch.size() < numberOfItineraries) {
        Flight f = new Flight(results.getInt("fid"), results.getInt("day_of_month"),
                              results.getString("carrier_id"), results.getString("flight_num"),
                              results.getString("origin_city"), results.getString("dest_city"),
                              results.getInt("actual_time"), results.getInt("capacity"),
                              results.getInt("price"));
        currSearch.add(new Itinerary(f));
      }

      
      if (!directFlight && currSearch.size() < numberOfItineraries) {
        twoHopStmt.setInt(1, numberOfItineraries - currSearch.size());
        twoHopStmt.setString(2, originCity);
        twoHopStmt.setString(3, destinationCity);
        twoHopStmt.setInt(4, dayOfMonth);
        
        results = twoHopStmt.executeQuery();
        while (results.next()) {
          Flight f1 = new Flight(results.getInt("f1_fid"), results.getInt("f1_day_of_month"),
                                 results.getString("f1_carrier_id"),
                                 results.getString("f1_flight_num"),
                                 results.getString("f1_origin_city"),
                                 results.getString("f1_dest_city"),
                                 results.getInt("f1_actual_time"),
                                 results.getInt("f1_capacity"), results.getInt("f1_price"));
          Flight f2 = new Flight(results.getInt("f2_fid"), results.getInt("f2_day_of_month"),
                                 results.getString("f2_carrier_id"),
                                 results.getString("f2_flight_num"),
                                 results.getString("f2_origin_city"),
                                 results.getString("f2_dest_city"),
                                 results.getInt("f2_actual_time"), results.getInt("f2_capacity"),
                                 results.getInt("f2_price"));
          currSearch.add(new Itinerary(f1, f2));
        }
        
        Collections.sort(currSearch);
      }

      if (currSearch.isEmpty()) {
        return "No flights match your selection\n";
      } else {
        StringBuilder sb = new StringBuilder();
        for (int id = 0; id < currSearch.size(); ++id) {
          Itinerary itn = currSearch.get(id);
          sb.append("Itinerary " + id + ": " + itn.getNumFlights() + " flight(s), "
                    + itn.totalDuration + " minutes\n");
          sb.append(itn);
        }
        return sb.toString();
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
      return FAILURE;
    }
  }

  
  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search
   *                    in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged
   *         in\n". If the user is trying to book an itinerary with an invalid ID or without
   *         having done a search, then return "No such itinerary {@code itineraryId}\n". If the
   *         user already has a reservation on the same day as the one that they are trying to
   *         book now, then return "You cannot book two flights in the same day\n". For all
   *         other errors, return "Booking failed\n".
   *
   *         If booking succeeds, return "Booked flight(s), reservation ID: [reservationId]\n"
   *         where reservationId is a unique number in the reservation system that starts from
   *         1 and increments by 1 each time a successful reservation is made by any user in
   *         the system.
   */
  public String transaction_book(int itineraryId) {
    if (currUser == null) {
      return "Cannot book reservations, not logged in\n";
    } else if (currSearch == null || currSearch.isEmpty() || itineraryId >= currSearch.size()) {
      return "No such itinerary " + itineraryId + "\n";
    }

    final String FAILURE = "Booking failed\n";
    Itinerary itn = currSearch.get(itineraryId);
    try {
      beginTransaction();
      ResultSet results;
      
      noOtherFlightsStmt.setString(1, currUser);
      noOtherFlightsStmt.setInt(2, itn.f1.dayOfMonth);
      results = noOtherFlightsStmt.executeQuery();
      if (results.next()) {
        rollbackTransaction();
        return "You cannot book two flights in the same day\n";
      }

      remainingCapacityStmt.setInt(1, itn.f1.fid);
      remainingCapacityStmt.setInt(2, itn.f1.fid);
      results = remainingCapacityStmt.executeQuery();
      if (results.next() && results.getInt("availability") <= 0) {
        rollbackTransaction();
        return FAILURE;
      }

      results = getNextReservationStmt.executeQuery();
      int nextId = 1;
      if (results.next()) {
        nextId = results.getInt("next_id") + 1;
      }

      PreparedStatement stmt = createOneFlightReservationStmt;
      if (!itn.isDirect()) {
        stmt = createTwoFlightReservationStmt;
        stmt.setInt(4, itn.f2.fid);
      }
      stmt.setInt(1, nextId);
      stmt.setString(2, currUser);
      stmt.setInt(3, itn.f1.fid);

      if (stmt.executeUpdate() != 1) {
        rollbackTransaction();
        return FAILURE;
      } else {
        commitTransaction();
        return "Booked flight(s), reservation ID: " + nextId + "\n";
      }
    } catch (SQLException ex) {
      try {
        ex.printStackTrace();
        rollbackTransaction();
        if (isDeadlock(ex)) {
          return transaction_book(itineraryId);
        } else {
          return FAILURE;
        }
      } catch (SQLException innerEx) {
    
        innerEx.printStackTrace();
        return FAILURE;
      }
    }
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n". If the
   *         reservation is not found / not under the logged in user's name, then return
   *         "Cannot find unpaid reservation [reservationId] under user: [username]\n".  If
   *         the user does not have enough money in their account, then return
   *         "User has only [balance] in account but itinerary costs [cost]\n".  For all other
   *         errors, return "Failed to pay for reservation [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining balance:
   *         [balance]\n" where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay(int reservationId) {
    if (currUser == null) {
      return "Cannot pay, not logged in\n";
    }

    final String FAILURE = "Failed to pay for reservation " + reservationId + "\n";
    try {
      beginTransaction();

      findPayableReservationStmt.setInt(1, reservationId);
      findPayableReservationStmt.setString(2, currUser);
      ResultSet results = findPayableReservationStmt.executeQuery();
      if (!results.next()) {
        rollbackTransaction();
        return "Cannot find unpaid reservation " + reservationId
          + " under user: " + currUser + "\n";
      }

      int price = results.getInt("price");
      if (results.getInt("fid2") != 0 || !results.wasNull()) {
        getFlightPriceStmt.setInt(1, results.getInt("fid2"));
        results = getFlightPriceStmt.executeQuery();
        if (!results.next()) {
          rollbackTransaction();
          return FAILURE;
        } else {
          price += results.getInt("price");
        }
      }

      getUserBalanceStmt.setString(1, currUser);
      results = getUserBalanceStmt.executeQuery();
      int newBalance = -1;
      if (!results.next()) {
        rollbackTransaction();
        return FAILURE;
      } else {
        int balance = results.getInt("balance");
        newBalance = balance - price;
        if (newBalance < 0) {
          rollbackTransaction();
          return "User has only " + balance + " in account but itinerary costs " + price + "\n";
        }
      }
      
      payReservationStmt.setInt(1, reservationId);
      payReservationStmt.setInt(2, newBalance);
      payReservationStmt.setString(3, currUser);
      if (payReservationStmt.executeUpdate() != 1) {
        rollbackTransaction();
        return FAILURE;
      } else {
        commitTransaction();
        return "Paid reservation: " + reservationId + " remaining balance: " + newBalance + "\n";
      }
    } catch (SQLException ex) {
      try {
        rollbackTransaction();
        if (isDeadlock(ex)) {
          return transaction_pay(reservationId);
        } else {
          return FAILURE;
        }
      } catch (SQLException innerEx) {
        innerEx.printStackTrace();
        return FAILURE;
      }
    }
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n" If
   *         the user has no reservations, then return "No reservations found\n" For all other
   *         errors, return "Failed to retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n [flight 1 under the
   *         reservation]\n [flight 2 under the reservation]\n Reservation [reservation ID] paid:
   *         [true or false]:\n [flight 1 under the reservation]\n [flight 2 under the
   *         reservation]\n ...
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    if (currUser == null) {
      return "Cannot view reservations, not logged in\n";
    }

    final String FAILURE = "Failed to retrieve reservations\n";
    try {
      beginTransaction();

      getReservationsStmt.setString(1, currUser);
      ResultSet results = getReservationsStmt.executeQuery();

      StringBuilder sb = new StringBuilder();
      while (results.next()) {
        sb.append("Reservation " + results.getInt("res_id") + " paid: "
                  + (results.getInt("paid") == 0 ? "false" : "true") + ":\n");
        Flight f = new Flight(results.getInt("fid1"), results.getInt("day_of_month"),
                              results.getString("carrier_id"), results.getString("flight_num"),
                              results.getString("origin_city"), results.getString("dest_city"),
                              results.getInt("actual_time"), results.getInt("capacity"),
                              results.getInt("price"));
        sb.append(f.toString());
        if (results.getInt("fid2") != 0 || !results.wasNull()) {
          getFlightInfoStmt.setInt(1, results.getInt("fid2"));
          ResultSet secondResults = getFlightInfoStmt.executeQuery();
          if (!secondResults.next()) {
            rollbackTransaction();
            return FAILURE;
          } else {
            f = new Flight(secondResults.getInt("fid"), secondResults.getInt("day_of_month"),
                           secondResults.getString("carrier_id"),
                           secondResults.getString("flight_num"),
                           secondResults.getString("origin_city"),
                           secondResults.getString("dest_city"),
                           secondResults.getInt("actual_time"), secondResults.getInt("capacity"),
                           secondResults.getInt("price"));
            sb.append("\n" + f.toString());
          }
        }
        sb.append("\n");
      }

      if (sb.length() == 0) {
        sb.append("No reservations found\n");
      }

      rollbackTransaction();  
      return sb.toString();
    } catch (SQLException ex) {
      try {
        ex.printStackTrace();
        rollbackTransaction();
        if (isDeadlock(ex)) {
          return transaction_reservations();
        } else {
          return FAILURE;
        }
      } catch (SQLException innerEx) {
        innerEx.printStackTrace();
        return FAILURE;
      }
    }
  }


  public void beginTransaction() throws SQLException {
    conn.setAutoCommit(false);
  }
  public void commitTransaction() throws SQLException {
    conn.commit();
    conn.setAutoCommit(true);
  }
  public void rollbackTransaction() throws SQLException {
    conn.rollback();
    conn.setAutoCommit(true);
  }


  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }
    
    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }


  class Itinerary implements Comparable<Itinerary> {
    public Flight f1, f2;
    public int totalDuration;

    public Itinerary(Flight f1) {
      this.f1 = f1;
      this.totalDuration = f1.time;
    }

    public Itinerary(Flight f1, Flight f2) {
      this.f1 = f1;
      this.f2 = f2;
      this.totalDuration = f1.time + f2.time;
    }

    public int getNumFlights() { return (f2 != null) ? 2 : 1; }
    public boolean isDirect() { return (f2 != null) ? false : true; }

    @Override
    public String toString() { return (f2 != null) ? (f1 + "\n" + f2 + "\n") : (f1 + "\n"); }

    @Override
    public int compareTo(Itinerary other) {
      if (this.totalDuration == other.totalDuration) {
        return this.f1.fid - other.f1.fid;
      } else {
        return this.totalDuration - other.totalDuration;
      }
    }
  }
}