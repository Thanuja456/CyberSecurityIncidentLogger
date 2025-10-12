import java.util.*;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.Document;
import static com.mongodb.client.model.Filters.eq;

public class CyberSecurityIncidentLogger {

    // ---------- Incident class ----------
    static class Incident {
        int id;
        String type;
        String description;
        String severity;
        String date;

        Incident(int id, String type, String description, String severity, String date) {
            this.id = id;
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.date = date;
        }

        @Override
        public String toString() {
            return "\nIncident ID: " + id +
                    "\nType: " + type +
                    "\nDescription: " + description +
                    "\nSeverity: " + severity +
                    "\nDate: " + date;
        }
    }

    // ---------- MongoDB helper ----------
    static class MongoBackup implements AutoCloseable {
        private final MongoClient client;
        private final MongoCollection<Document> collection;

        MongoBackup(String uri, String dbName) {
            ConnectionString conn = new ConnectionString(uri);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(conn)
                    .build();
            client = MongoClients.create(settings);
            MongoDatabase db = client.getDatabase(dbName);
            collection = db.getCollection("incidents");
        }

        void backupIncident(Incident inc) {
            Document doc = new Document("id", inc.id)
                    .append("type", inc.type)
                    .append("description", inc.description)
                    .append("severity", inc.severity)
                    .append("date", inc.date);
            collection.insertOne(doc);
        }

        void deleteIncident(int id) {
            collection.deleteOne(eq("id", id));
        }

        void showAll() {
            for (Document d : collection.find()) {
                System.out.println(d.toJson());
            }
        }

        @Override
        public void close() {
            client.close();
        }
    }

    // ---------- Main Program ----------
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ArrayList<Incident> list = new ArrayList<>();
        int choice, idCounter = 1;

        // Use your own MongoDB URI:
        // Local: "mongodb://localhost:27017"
        // Atlas: "mongodb+srv://<username>:<password>@cluster0.mongodb.net/"
        String uri = "mongodb://localhost:27017";

        try (MongoBackup mongo = new MongoBackup(uri, "cyber_security_logger")) {

            do {
                System.out.println("\n=== Cyber Security Incident Logger ===");
                System.out.println("1. Log New Incident");
                System.out.println("2. View Local Incidents");
                System.out.println("3. View MongoDB Backup");
                System.out.println("4. Delete Incident");
                System.out.println("5. Exit");
                System.out.print("Enter choice: ");
                choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1 -> {
                        System.out.print("Enter Type: ");
                        String type = sc.nextLine();
                        System.out.print("Enter Description: ");
                        String desc = sc.nextLine();
                        System.out.print("Enter Severity (Low/Medium/High): ");
                        String sev = sc.nextLine();
                        System.out.print("Enter Date (DD-MM-YYYY): ");
                        String date = sc.nextLine();

                        Incident inc = new Incident(idCounter++, type, desc, sev, date);
                        list.add(inc);
                        mongo.backupIncident(inc);
                        System.out.println("✅ Incident logged and backed up!");
                    }

                    case 2 -> {
                        if (list.isEmpty()) System.out.println("No local incidents found.");
                        else list.forEach(System.out::println);
                    }

                    case 3 -> {
                        System.out.println("--- MongoDB Backup ---");
                        mongo.showAll();
                    }

                    case 4 -> {
                        System.out.print("Enter ID to delete: ");
                        int id = sc.nextInt();
                        sc.nextLine();
                        list.removeIf(i -> i.id == id);
                        mongo.deleteIncident(id);
                        System.out.println("🗑️ Incident deleted (local + MongoDB).");
                    }

                    case 5 -> System.out.println("Exiting...");
                    default -> System.out.println("Invalid choice.");
                }
            } while (choice != 5);

        } catch (Exception e) {
            e.printStackTrace();
        }

        sc.close();
    }
}
