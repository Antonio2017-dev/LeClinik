import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.Scanner;

public class MainClinik {
    private static final LinkedQueue<Patient> waitingQueue = new LinkedQueue<>();
    private static final LinkedQueue<Patient> attentionQueue = new LinkedQueue<>();

    // Receipt file: append mode + timestamped session separator
    private static final String RECEIPT_FILE = "receipts.txt";

    public static void main(String[] args) {
        // Allow passing a custom data path; default to Patient
        Path patientFile = Paths.get(args != null && args.length > 0 ? args[0] : "Patient.txt");

        if (!Files.exists(patientFile)) {
            System.err.println("Data file not found: " + patientFile.toAbsolutePath());
            System.err.println("Expected CSV lines like:");
            System.err.println("id,name,MM/dd/yyyy,phone,address,reason");
            System.err.println("Example: 1,Nehemias,09/28/1968,862-281-4373,Illinois,General Check");
        } else {
            try {
                loadPatients(patientFile);
            } catch (Exception e) {
                System.err.println("Error loading patients: " + e.getMessage());
            }
        }

        try (Scanner in = new Scanner(System.in)) {
            boolean exit = false;
            System.out.println("Welcome to Le Clinik!\n");

            while (!exit) {
                printMenu();
                int option = readInt(in, "Choose an option: ");

                try {
                    switch (option) {
                        case 1:
                            viewQueue("Waiting Room", waitingQueue);
                            break;
                        case 2: {
                            int n = readInt(in, "How many patients do you want to pass to attention? ");
                            passPatients(n);
                            break;
                        }
                        case 3:
                            viewQueue("Being Attended", attentionQueue);
                            break;
                        case 4: {
                            System.out.print("Type the ID: ");
                            String bid = in.next().trim();
                            searchById(bid, true); // search both queues
                            break;
                        }
                        case 5: {
                            int n = readInt(in, "How many patients to process (charge and remove from attention)? ");
                            processPatients(n);
                            break;
                        }
                        case 6:
                            exit = true;
                            System.out.println("Goodbye!");
                            break;
                        default:
                            System.err.println("Enter a valid option (1-6).");
                    }
                } catch (UnderflowExcep ue) {
                    System.err.println("Queue underflow: " + ue.getMessage());
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }

                System.out.println();
            }
        }
    }

    private static void printMenu() {
        System.out.println("---------------------------------------");
        System.out.println("Type a number and press 'Enter'.");
        System.out.println("---------------------------------------");
        System.out.println("1. View patients in waiting room");
        System.out.println("2. Pass patients to attention");
        System.out.println("3. View patients in attention");
        System.out.println("4. Search patient by ID");
        System.out.println("5. Process patients (create receipt)");
        System.out.println("6. Exit");
        System.out.println();
    }

    private static int readInt(Scanner in, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = in.nextInt();
                in.nextLine(); // consume newline
                return v;
            } catch (InputMismatchException e) {
                in.nextLine(); // clear invalid token
                System.err.println("Please enter a valid integer.");
            }
        }
    }

    /** Load patients from CSV-style file: id,name,MM/dd/yyyy,phone,address,reason */
    private static void loadPatients(Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                String[] v = line.split(",");
                if (v.length < 6) {
                    System.err.println("Skipping malformed line " + lineNo + ": " + line);
                    continue;
                }
                try {
                    Patient p = new Patient(
                            v[0].trim(), // id
                            v[1].trim(), // names
                            v[2].trim(), // birthdate MM/dd/yyyy
                            v[3].trim(), // phone
                            v[4].trim(), // address
                            v[5].trim()  // reason
                    );
                    waitingQueue.enqueue(p);
                } catch (Exception e) {
                    System.err.println("Skipping line " + lineNo + " due to parse error: " + e.getMessage());
                }
            }
        }
    }

    /** Pretty-print a queue without losing order. */
    private static void viewQueue(String title, LinkedQueue<Patient> q) throws UnderflowExcep {
        System.out.println("=== " + title + " (" + q.size() + ") ===");
        int n = q.size();
        for (int i = 0; i < n; i++) {
            Patient p = q.dequeue();
            System.out.println(
                p.getNames() + " | " + p.getReasonOfVisit() + " | Check-in: " + p.getCheckInTime()
                + " | Age: " + p.getAgeYears()
            );
            q.enqueue(p); // rotate to preserve original order
        }
    }

    /** Move up to n patients from waiting to attention. */
    private static void passPatients(int n) throws UnderflowExcep {
        if (n <= 0) {
            System.err.println("Number must be positive.");
            return;
        }
        int moved = 0;
        while (moved < n && !waitingQueue.isEmpty()) {
            Patient p = waitingQueue.dequeue();
            System.out.println("Passing: " + p.getNames() + " | " + p.getReasonOfVisit() + " | Check-in: " + p.getCheckInTime());
            attentionQueue.enqueue(p);
            moved++;
        }
        if (moved < n) {
            System.out.println("Only " + moved + " patient(s) available to pass.");
        }
    }

    /** Find a patient by ID in waiting and optionally attention queues (non-destructive). */
    private static void searchById(String id, boolean includeAttention) throws UnderflowExcep {
        boolean found = false;

        // Search waiting
        int n = waitingQueue.size();
        for (int i = 0; i < n; i++) {
            Patient p = waitingQueue.dequeue();
            if (p.getId().equals(id)) {
                System.out.println("Found in Waiting: " + p);
                found = true;
            }
            waitingQueue.enqueue(p);
        }

        // Optionally search attention
        if (includeAttention) {
            int m = attentionQueue.size();
            for (int i = 0; i < m; i++) {
                Patient p = attentionQueue.dequeue();
                if (p.getId().equals(id)) {
                    System.out.println("Found in Attention: " + p);
                    found = true;
                }
                attentionQueue.enqueue(p);
            }
        }

        if (!found) System.out.println("No patient with ID " + id + " found.");
    }

    /** Process first n patients in attention: write receipt lines and remove them. */
    private static void processPatients(int n) throws UnderflowExcep, IOException {
        if (n <= 0) {
            System.err.println("Number must be positive.");
            return;
        }
        if (attentionQueue.isEmpty()) {
            System.out.println("No patients in attention to process.");
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String sessionHeader = "\n=== Receipt Session @ " + timestamp + " ===\n";

        double total = 0.0;
        StringBuilder builder = new StringBuilder();
        builder.append(sessionHeader);

        int processed = 0;
        while (processed < n && !attentionQueue.isEmpty()) {
            Patient p = attentionQueue.dequeue();
            double price = p.getReasonOfVisit().price();
            builder.append(String.format("%s | ID:%s | %s | $%.2f%n",
                    p.getNames(), p.getId(), p.getReasonOfVisit().label(), price));
            total += price;
            processed++;
        }

        builder.append(String.format("Total: $%.2f%n", total));

        try (FileWriter fw = new FileWriter(RECEIPT_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(builder.toString());
        }

        System.out.println("Processed " + processed + " patient(s). Receipt appended to " + RECEIPT_FILE);
        if (processed < n) {
            System.out.println("Only " + processed + " patient(s) were available to process.");
        }
    }
}
