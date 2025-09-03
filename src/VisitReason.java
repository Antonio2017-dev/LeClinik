public enum VisitReason {
    GENERAL_CHECK(20.0, "General Check"),
    PRESCRIPTION(30.0, "Prescription"),
    INTERVENTION(50.0, "Intervention");

    private final double price;
    private final String label;

    VisitReason(double price, String label) {
        this.price = price;
        this.label = label;
    }

    public double price() { return price; }
    public String label() { return label; }

    public static VisitReason parse(String text) {
        String t = text == null ? "" : text.trim().toLowerCase();
        switch (t) {
            case "general check": return GENERAL_CHECK;
            case "prescription":  return PRESCRIPTION;
            case "intervention":  return INTERVENTION;
            default:              return GENERAL_CHECK; // sensible default
        }
    }

    @Override
    public String toString() { return label; }
}