package scripts.databaseApproach

import java.util.concurrent.ConcurrentHashMap

class Database {
    static Map db = new ConcurrentHashMap()

    String toString () {
        "Database [size:$db.size()]"
    }
}
