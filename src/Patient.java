import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Patient implements Comparable<Patient> {
    private static final DateTimeFormatter FLEX_MDYYYY = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('/')
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral('/')
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter();

    private final LocalTime checkInTime; // NEW: stable check-in time

    private String id;
    private String names;
    private LocalDate birthDate;
    private String phoneNumber;
    private String address;
    private VisitReason reasonOfVisit;

    public Patient(String id, String names, String birthDateMDY, String phoneNumber, String address, String reasonOfVisit) {
        this.id = id;
        this.names = names;
        this.birthDate = LocalDate.parse(birthDateMDY.trim(), FLEX_MDYYYY);
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.reasonOfVisit = VisitReason.parse(reasonOfVisit);
        this.checkInTime = LocalTime.now();
    }

    /** Backwards-compatible current time (kept if someone uses it elsewhere). */
    public String getTime() {
        Date now = new Date();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        return timeFormat.format(now);
    }

    /** NEW: Fixed check-in time captured at construction. */
    public String getCheckInTime() {
        return checkInTime.toString();
    }

    public String getId() { return id; }
    public void setId(String newId) { this.id = newId; }

    public String getNames() { return names; }
    public void setNames(String names) { this.names = names; }

    public String getPhoneNumber() {
        String digits = phoneNumber.replaceAll("\\D+", "");
        if (digits.length() == 10) {
            return String.format("(%s) %s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6));
        }
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public VisitReason getReasonOfVisit() { return reasonOfVisit; }
    public void setReasonOfVisit(VisitReason reasonOfVisit) { this.reasonOfVisit = reasonOfVisit; }

    public LocalDate getBirthDate() { return birthDate; }

    /** Age in full years. */
    public int getAgeYears() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    @Override
    public String toString() {
        return "Patient [Names: " + names
                + ", ID: " + id
                + ", Birthdate: " + birthDate
                + ", Phone: " + getPhoneNumber()
                + ", Address: " + address
                + ", Reason: " + reasonOfVisit
                + ", Check-in: " + getCheckInTime()
                + "]";
    }

    /** Sort older first (by age years). */
    @Override
    public int compareTo(Patient other) {
        return Integer.compare(other.getAgeYears(), this.getAgeYears());
    }
}
