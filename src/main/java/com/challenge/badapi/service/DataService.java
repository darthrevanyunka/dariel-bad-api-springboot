package com.challenge.badapi.service;

import com.challenge.badapi.model.Person;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.*;

@Service
public class DataService {

    private final List<Person> people = new ArrayList<>();
    private final Map<Long, Person> peopleById = new HashMap<>();

    private static final String[] FIRST_NAMES = {
            // English names
            "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
            "William", "Barbara", "David", "Elizabeth", "Richard", "Susan", "Joseph", "Jessica",
            "Thomas", "Sarah", "Charles", "Karen", "Christopher", "Nancy", "Daniel", "Lisa",
            "Matthew", "Betty", "Anthony", "Margaret", "Mark", "Sandra", "Donald", "Ashley",
            "Steven", "Kimberly", "Paul", "Emily", "Andrew", "Donna", "Joshua", "Michelle",
            "Kenneth", "Dorothy", "Kevin", "Carol", "Brian", "Amanda", "George", "Melissa",
            "Edward", "Deborah", "Ronald", "Stephanie", "Timothy", "Rebecca", "Jason", "Sharon",
            "Jeffrey", "Laura", "Ryan", "Cynthia", "Jacob", "Kathleen", "Gary", "Amy",
            "Nicholas", "Shirley", "Eric", "Angela", "Jonathan", "Helen", "Stephen", "Anna",
            "Larry", "Brenda", "Justin", "Pamela", "Scott", "Nicole", "Brandon", "Emma",
            "Benjamin", "Samantha", "Samuel", "Katherine", "Raymond", "Christine", "Gregory", "Debra",
            "Frank", "Rachel", "Alexander", "Catherine", "Patrick", "Carolyn", "Jack", "Janet",
            "Dennis", "Ruth", "Jerry", "Maria", "Tyler", "Heather", "Aaron", "Diane",
            // German names
            "Hans", "Anna", "Klaus", "Greta", "Wolfgang", "Heidi", "Dieter", "Ingrid",
            "Jurgen", "Petra", "Helmut", "Sabine", "Gunther", "Monika", "Rolf", "Brigitte",
            "Fritz", "Ursula", "Otto", "Renate", "Karl", "Erika", "Horst", "Christa",
            "Werner", "Angelika", "Manfred", "Gabriele", "Bernd", "Helga", "Uwe", "Ilse",
            "Heinrich", "Gerda", "Ludwig", "Hannelore", "Rainer", "Irmgard", "Ernst", "Hildegard",
            // Hindi/Indian names
            "Raj", "Priya", "Amit", "Anjali", "Arjun", "Kavya", "Rohan", "Diya",
            "Vikram", "Ishita", "Karan", "Neha", "Aditya", "Pooja", "Ravi", "Sita",
            "Suresh", "Lakshmi", "Ajay", "Radha", "Ramesh", "Deepa", "Vijay", "Meera",
            "Krishna", "Parvati", "Mohan", "Sanjana", "Prakash", "Shreya", "Ashok", "Ananya",
            "Sandeep", "Divya", "Rahul", "Nisha", "Anil", "Swati", "Rajesh", "Geeta",
            // Spanish names
            "Carlos", "Carmen", "Jose", "Isabella", "Juan", "Sofia", "Luis", "Maria",
            "Miguel", "Ana", "Diego", "Laura", "Fernando", "Rosa", "Pablo", "Elena",
            "Pedro", "Lucia", "Jorge", "Marta", "Alberto", "Beatriz", "Raul", "Cristina",
            // French names
            "Pierre", "Marie", "Jean", "Sophie", "Jacques", "Isabelle", "Michel", "Catherine",
            "Philippe", "Nathalie", "Henri", "Helene", "Francois", "Sylvie", "Laurent", "Monique",
            // Italian names
            "Giovanni", "Giulia", "Marco", "Francesca", "Antonio", "Chiara", "Luca", "Valentina",
            "Giuseppe", "Alessandra", "Francesco", "Elena", "Matteo", "Silvia", "Andrea", "Laura",
            // Chinese names (romanized)
            "Wei", "Ling", "Ming", "Yan", "Chen", "Mei", "Li", "Xia",
            "Wang", "Hui", "Zhang", "Fang", "Liu", "Lei", "Yang", "Jing",
            // Japanese names
            "Hiroshi", "Yuki", "Takeshi", "Sakura", "Kenji", "Akiko", "Taro", "Hanako",
            "Kazuo", "Midori", "Hideo", "Yoko", "Masaru", "Keiko", "Ichiro", "Naomi",
            // Arabic names
            "Ahmed", "Fatima", "Mohammed", "Aisha", "Ali", "Zahra", "Omar", "Layla",
            "Hassan", "Maryam", "Ibrahim", "Zainab", "Khalid", "Nour", "Said", "Amina",
            // Russian names
            "Ivan", "Olga", "Dmitri", "Natasha", "Sergei", "Elena", "Vladimir", "Irina",
            "Alexei", "Tatiana", "Nikolai", "Svetlana", "Boris", "Anya", "Mikhail", "Katya",
            // Scandinavian names
            "Lars", "Ingrid", "Bjorn", "Astrid", "Sven", "Helga", "Erik", "Sigrid",
            "Anders", "Karin", "Magnus", "Freya", "Olaf", "Liv", "Thor", "Solveig",
            // African names
            "Kwame", "Ama", "Kofi", "Akua", "Jabari", "Zuri", "Chidi", "Amara",
            "Mandla", "Thandiwe", "Kwesi", "Nala", "Sekou", "Nia", "Jamal", "Ayanna"
    };

    private static final String[] SURNAMES = {
            // English surnames
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Wilson",
            "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Thompson", "White",
            "Harris", "Clark", "Lewis", "Robinson", "Walker", "Young", "Allen", "King",
            "Wright", "Scott", "Hill", "Green", "Adams", "Nelson", "Baker", "Hall",
            "Campbell", "Mitchell", "Carter", "Roberts", "Phillips", "Evans", "Turner", "Parker",
            "Edwards", "Collins", "Stewart", "Morris", "Murphy", "Cook", "Rogers", "Morgan",
            "Cooper", "Peterson", "Bailey", "Reed", "Kelly", "Howard", "Cox", "Ward",
            "Richardson", "Watson", "Brooks", "Wood", "James", "Bennett", "Gray", "Hughes",
            "Price", "Sanders", "Myers", "Long", "Ross", "Foster", "Butler", "Barnes",
            // German surnames
            "Mueller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker",
            "Schulz", "Hoffmann", "Schaefer", "Koch", "Bauer", "Richter", "Klein", "Wolf",
            "Schroeder", "Neumann", "Schwarz", "Zimmermann", "Braun", "Krueger", "Hartmann", "Lange",
            "Schmitt", "Werner", "Krause", "Meier", "Lehmann", "Schmid", "Schulze", "Maier",
            // Spanish surnames
            "Garcia", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Perez", "Sanchez",
            "Ramirez", "Torres", "Flores", "Rivera", "Gomez", "Diaz", "Cruz", "Reyes",
            "Morales", "Gutierrez", "Ortiz", "Ramos", "Chavez", "Mendoza", "Ruiz", "Alvarez",
            "Castillo", "Jimenez", "Moreno", "Romero", "Herrera", "Medina", "Aguilar", "Vargas",
            // Italian surnames
            "Rossi", "Russo", "Ferrari", "Esposito", "Bianchi", "Romano", "Colombo", "Ricci",
            "Marino", "Greco", "Bruno", "Gallo", "Conti", "DeLuca", "Mancini", "Costa",
            "Giordano", "Rizzo", "Lombardi", "Moretti", "Barbieri", "Fontana", "Santoro", "Marini",
            // French surnames
            "Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit", "Durand",
            "Leroy", "Moreau", "Simon", "Laurent", "Lefebvre", "Michel", "Garcia", "David",
            "Bertrand", "Roux", "Vincent", "Fournier", "Morel", "Girard", "Andre", "Mercier",
            // Indian surnames
            "Patel", "Singh", "Kumar", "Sharma", "Gupta", "Khan", "Reddy", "Das",
            "Verma", "Joshi", "Rao", "Nair", "Iyer", "Pillai", "Menon", "Desai",
            "Mishra", "Agarwal", "Jain", "Mehta", "Shah", "Bose", "Mukherjee", "Malhotra",
            // Chinese surnames
            "Wang", "Li", "Zhang", "Liu", "Chen", "Yang", "Huang", "Zhao",
            "Wu", "Zhou", "Xu", "Sun", "Ma", "Zhu", "Hu", "Guo",
            "He", "Gao", "Lin", "Luo", "Zheng", "Liang", "Song", "Tang",
            // Japanese surnames
            "Sato", "Suzuki", "Takahashi", "Tanaka", "Watanabe", "Ito", "Yamamoto", "Nakamura",
            "Kobayashi", "Kato", "Yoshida", "Yamada", "Sasaki", "Yamaguchi", "Matsumoto", "Inoue",
            // Korean surnames
            "Kim", "Lee", "Park", "Choi", "Jung", "Kang", "Cho", "Yoon",
            "Jang", "Lim", "Han", "Oh", "Seo", "Shin", "Kwon", "Song",
            // Arabic surnames
            "Ahmed", "Ali", "Hassan", "Mohammed", "Ibrahim", "Khalil", "Mahmoud", "Abdullah",
            "Rahman", "Hamid", "Hussein", "Karim", "Salem", "Mansour", "Omar", "Youssef",
            // Russian surnames
            "Ivanov", "Smirnov", "Kuznetsov", "Popov", "Sokolov", "Lebedev", "Kozlov", "Novikov",
            "Morozov", "Petrov", "Volkov", "Solovyov", "Vasiliev", "Zaitsev", "Pavlov", "Mikhailov",
            // Scandinavian surnames
            "Andersen", "Hansen", "Johansen", "Olsen", "Larsen", "Nilsen", "Pedersen", "Kristiansen",
            "Jensen", "Karlsen", "Eriksen", "Berg", "Haugen", "Dahl", "Lund", "Moen",
            // African surnames
            "Nkosi", "Okafor", "Adebayo", "Mensah", "Banda", "Mwangi", "Kamau", "Ochieng",
            "Diallo", "Traore", "Sesay", "Koroma", "Moyo", "Khumalo", "Zulu", "Dlamini",
            // Vietnamese surnames
            "Nguyen", "Tran", "Le", "Pham", "Hoang", "Phan", "Vu", "Dang",
            "Bui", "Do", "Ngo", "Duong", "Ly", "Vo", "Truong", "Lam"
    };

    private static final int PEOPLE_COUNT = 2000;

    @PostConstruct
    public void init() {
        // Generate deterministic people using a fixed seed for reproducibility
        Random random = new Random(42); // Fixed seed for deterministic generation

        // Validation matches CSV rows back to people by (firstName, surname, age) alone,
        // so every combination generated here must be unique or a correct submission
        // could be rejected as a false "duplicate".
        Set<String> usedCombinations = new HashSet<>();
        long nextId = 1;

        while (people.size() < PEOPLE_COUNT) {
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String surname = SURNAMES[random.nextInt(SURNAMES.length)];
            int age = 18 + random.nextInt(63); // Age between 18 and 80

            String combinationKey = firstName + "|" + surname + "|" + age;
            if (!usedCombinations.add(combinationKey)) {
                continue; // collision - redraw
            }

            Person person = new Person(nextId, firstName, surname, age);
            people.add(person);
            peopleById.put(nextId, person);
            nextId++;
        }

        System.out.println("✓ Generated " + PEOPLE_COUNT + " unique people records with diverse international names");
    }

    public List<Person> getAllPeople() {
        return new ArrayList<>(people);
    }

    public Person getPersonById(Long id) {
        return peopleById.get(id);
    }

    public List<Person> getPeopleByIds(List<Long> ids) {
        List<Person> result = new ArrayList<>();
        for (Long id : ids) {
            Person person = peopleById.get(id);
            if (person != null) {
                result.add(person);
            }
        }
        return result;
    }

    public int getTotalCount() {
        return people.size();
    }

    public List<Person> getPeoplePage(int offset, int limit) {
        int fromIndex = Math.min(offset, people.size());
        int toIndex = Math.min(offset + limit, people.size());
        return new ArrayList<>(people.subList(fromIndex, toIndex));
    }
}

