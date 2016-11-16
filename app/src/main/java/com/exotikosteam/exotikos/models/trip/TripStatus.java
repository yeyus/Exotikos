package com.exotikosteam.exotikos.models.trip;

import com.exotikosteam.exotikos.models.ExotikosDatabase;
import com.exotikosteam.exotikos.models.flightstatus.FlightStatus;
import com.exotikosteam.exotikos.models.flightstatus.ScheduledFlight;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ModelContainer;
import com.raizlabs.android.dbflow.annotation.OneToMany;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lramaswamy on 11/9/16.
 */

@Parcel(analyze = {TripStatus.class})
@ModelContainer
@Table(database = ExotikosDatabase.class)
public class TripStatus extends BaseModel {

    @Column
    @PrimaryKey(autoincrement = true)
    Integer id;

    @Column(name = "passenger_name")
    String passengerName;

    @Column
    Integer flightStepOrdinal;

    FlightStep flightStep;

    @Column
    Integer currentFlight;

    List<Flight> flights = new ArrayList<>();

    //Empty constructor for Parceler
    public TripStatus() {
        super();
    }

    public TripStatus(List<Flight> flights) {
        this.flights = flights;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public void setFlightStep(FlightStep flightStep) {
        this.flightStepOrdinal = flightStep.ordinal();
        this.flightStep = flightStep;
    }

    @OneToMany(methods = OneToMany.Method.ALL, variableName = "flights")
    public List<Flight> getFlights() {
        if (flights == null || flights.size() < 1) {
            flights = SQLite.select()
                    .from(Flight.class)
                    .where(Flight_Table.trip_id.eq(id))
                    .orderBy(Flight_Table.order, true)
                    .queryList();
        }
        return flights;
    }

    public void setFlights(List<Flight> flights) {
        this.flights = flights;
    }

    public FlightStep getFlightStep() {
        return flightStep;
    }

    public Integer getCurrentFlight() {
        return currentFlight;
    }

    public void setCurrentFlight(Integer currentFlight) {
        this.currentFlight = currentFlight;
    }

    public static TripStatus get(Integer id) {
        return SQLite.select().from(TripStatus.class).where(TripStatus_Table.id.eq(id)).querySingle();
    }

    public static List<TripStatus> getAll() {
        return SQLite.select().from(TripStatus.class).queryList();
    }

    public static void persist(TripStatus tripStatus) {
        for (Flight f: tripStatus.getFlights()) {
            f.save();
        }
        tripStatus.save();
    }

    public static TripStatus newMockInstance() {
        TripStatus trip = new TripStatus();
        trip.setFlightStep(FlightStep.CHECK_IN);
        List<Flight> flights = new ArrayList<Flight>();
        //String arrivalDate, String departureDate, String flightNumber, String departureTime,
        // String departureTerminal, String arrivalTime, String arrivalTerminal, String seatNumber
        flights.add(0, Flight.newInstance("", "January 19, 2017 ", "BX456", "6:44 AM", "A14", "12:30 PM","", "23B"));
        flights.add(1, Flight.newInstance("January 19, 2017", "January 19, 2017", "ZK250", "1:44 PM", "B9", "3:45 PM","A23", "6C"));
        flights.add(2, Flight.newInstance("January 19, 2017", "January 19, 2017", "BN05", "6:44 PM", "C34", "8:45 PM","D43", "2B"));
        trip.setFlights(flights);
        return trip;
    }

    public static void saveFlight() {

    }

    public static TripStatus saveTrip(TripStatus trip, ScheduledFlight flight) {
        if (trip == null || trip.getId() == null) {
            return createTrip(Arrays.asList(flight));
        }
        return updateTrip(trip, new Flight(flight));
    }



    public static TripStatus saveTrip(TripStatus trip, FlightStatus flight, String seatNo) {
        if (trip == null || trip.getId() == null) {
            return createTrip(flight);
        }
        return updateTrip(trip, new Flight(flight, seatNo));
    }

    public static TripStatus updateTrip(TripStatus trip, Flight flight) {
        boolean isNewFlight = true;
        for (int i = 0; i < trip.getFlights().size(); i++) {
            Flight f = trip.getFlights().get(i);
            if (f.getFlightNumber() == flight.getFlightNumber() && f.getDepartureAirportIATA() == flight.getDepartureAirportIATA()) {
                Flight.mergeFlights(f, flight);
                isNewFlight = false;
                break;
            }
        }
        if (isNewFlight) {
            flight.setOrder(trip.getFlights().size());
            flight.setTripId(trip.getId());
            trip.getFlights().add(flight);
            flight.save();
        }
        trip.save();
        return trip;
    }

    public static TripStatus createTrip(List<ScheduledFlight> flights) {
        TripStatus trip = new TripStatus();
        trip.setFlightStep(FlightStep.PREPARATION);
        trip.setCurrentFlight(0);
        trip.save();
        int i = 0;
        for (ScheduledFlight s: flights) {
            Flight f = new Flight(s);
            f.setTripId(trip.getId());
            f.setOrder(++i);
            f.save();
        }
        return TripStatus.get(trip.getId());
    }


    private static TripStatus createTrip(FlightStatus flightStatus) {
        TripStatus trip = null;
        Flight flight = new Flight(flightStatus, null);
        trip = new TripStatus();
        trip.setFlightStep(FlightStep.PREPARATION);
        trip.setCurrentFlight(0);
        trip.save();
        flight.setTripId(trip.getId());
        flight.setOrder(0);
        flight.save();
        return TripStatus.get(trip.getId());
    }

}
