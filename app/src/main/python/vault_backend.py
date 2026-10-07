import json
import sqlite3

_connection = None


def _result(status, **values):
    return json.dumps({"status": status, **values})


def _ensure_connection():
    if _connection is None:
        raise RuntimeError("Database not initialized")
    return _connection


def update_master_password_data(salt: str, verification_hash: str, updates_json: str):
    try:
        import json
        items = json.loads(updates_json)
        connection = _ensure_connection()
        connection.execute("UPDATE vault_config SET salt=?, verification_hash=? WHERE id=1", (salt, verification_hash))
        for item in items:
            connection.execute("UPDATE vault_items SET encrypted_secret=?, updated_at=CURRENT_TIMESTAMP WHERE id=?", (item['secret'], item['id']))
        connection.commit()
        return _result("success")
    except Exception as error:
        return _result("error", error=str(error))

def init_db(db_path: str):
    global _connection
    try:
        _connection = sqlite3.connect(db_path)
        _connection.row_factory = sqlite3.Row
        
        # 1. Ensure config table exists
        _connection.executescript('''
            CREATE TABLE IF NOT EXISTS vault_config (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                salt TEXT NOT NULL,
                verification_hash TEXT NOT NULL
            );
        ''')
        
        # 2. Check for legacy incompatible schemas
        _connection.execute("CREATE TABLE IF NOT EXISTS vault_items (id INTEGER PRIMARY KEY AUTOINCREMENT)")
        cursor = _connection.execute("PRAGMA table_info(vault_items)")
        columns = [row[1] for row in cursor.fetchall()]
        
        if 'domain' in columns:
            # We have the old schema. Migrate data and drop the old table.
            _connection.executescript('''
                ALTER TABLE vault_items RENAME TO vault_items_legacy_backup;
                
                CREATE TABLE vault_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    website TEXT NOT NULL DEFAULT '',
                    username TEXT NOT NULL DEFAULT '',
                    encrypted_secret TEXT NOT NULL DEFAULT '',
                    category TEXT NOT NULL DEFAULT '',
                    is_favorite INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    website_url TEXT NOT NULL DEFAULT ''
                );
                
                INSERT INTO vault_items (id, website, username, encrypted_secret, created_at)
                SELECT id, domain, username, encrypted_password, created_at FROM vault_items_legacy_backup;
                
                DROP TABLE vault_items_legacy_backup;
            ''')
            # Refresh columns after migration
            cursor = _connection.execute("PRAGMA table_info(vault_items)")
            columns = [row[1] for row in cursor.fetchall()]
            
        # 3. Add any missing columns dynamically (SQLite ALTER TABLE ADD COLUMN) for normal updates
        expected_columns = {
            'website': "TEXT NOT NULL DEFAULT ''",
            'username': "TEXT NOT NULL DEFAULT ''",
            'encrypted_secret': "TEXT NOT NULL DEFAULT ''",
            'category': "TEXT NOT NULL DEFAULT ''",
            'is_favorite': "INTEGER NOT NULL DEFAULT 0",
            'created_at': "TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP",
            'updated_at': "TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP",
            'website_url': "TEXT NOT NULL DEFAULT ''"
        }
        
        for col, col_def in expected_columns.items():
            if col not in columns:
                try:
                    _connection.execute(f"ALTER TABLE vault_items ADD COLUMN {col} {col_def}")
                except Exception as e:
                    pass
                    
        _connection.commit()
        return _result("success")
    except Exception as error:
        return _result("error", error=str(error))


def vault_exists():
    try:
        row = _ensure_connection().execute("SELECT 1 FROM vault_config WHERE id = 1").fetchone()
        return _result("success", exists=row is not None)
    except Exception as error:
        return _result("error", error=str(error), exists=False)


def get_master_config():
    try:
        row = _ensure_connection().execute("SELECT salt, verification_hash FROM vault_config WHERE id = 1").fetchone()
        return _result("success", data=dict(row) if row else None)
    except Exception as error:
        return _result("error", error=str(error))


def setup_vault(salt: str, verification_hash: str):
    try:
        connection = _ensure_connection()
        connection.execute(
            "INSERT OR REPLACE INTO vault_config (id, salt, verification_hash) VALUES (1, ?, ?)",
            (salt, verification_hash),
        )
        connection.commit()
        return _result("success")
    except Exception as error:
        return _result("error", error=str(error))


def add_credential(website: str, username: str, encrypted_secret: str, category: str = "", website_url: str = ""):
    try:
        connection = _ensure_connection()
        cursor = connection.execute(
            "INSERT INTO vault_items (website, username, encrypted_secret, category, website_url) VALUES (?, ?, ?, ?, ?)",
            (website, username, encrypted_secret, category, website_url),
        )
        connection.commit()
        return _result("success", id=cursor.lastrowid)
    except Exception as error:
        return _result("error", error=str(error))


def list_credentials(favorites_only: bool = False):
    try:
        sql = "SELECT id, website, username, category, is_favorite, created_at, updated_at, website_url FROM vault_items"
        if favorites_only:
            sql += " WHERE is_favorite = 1"
        rows = _ensure_connection().execute(sql + " ORDER BY website COLLATE NOCASE").fetchall()
        return _result("success", data=[dict(row) for row in rows])
    except Exception as error:
        return _result("error", error=str(error), data=[])


def get_credential(credential_id: int):
    try:
        row = _ensure_connection().execute("SELECT * FROM vault_items WHERE id = ?", (credential_id,)).fetchone()
        return _result("success", data=dict(row) if row else None)
    except Exception as error:
        return _result("error", error=str(error))


def update_credential(credential_id: int, website: str, username: str, encrypted_secret: str, category: str, website_url: str = ""):
    try:
        connection = _ensure_connection()
        connection.execute(
            "UPDATE vault_items SET website=?, username=?, encrypted_secret=?, category=?, website_url=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
            (website, username, encrypted_secret, category, website_url, credential_id),
        )
        connection.commit()
        return _result("success")
    except Exception as error:
        return _result("error", error=str(error))


def delete_credential(credential_id: int):
    try:
        connection = _ensure_connection()
        connection.execute("DELETE FROM vault_items WHERE id = ?", (credential_id,))
        connection.commit()
        return _result("success")
    except Exception as error:
        return _result("error", error=str(error))


def toggle_favorite(credential_id: int, is_favorite: bool):
    try:
        connection = _ensure_connection()
        connection.execute(
            "UPDATE vault_items SET is_favorite=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
            (1 if is_favorite else 0, credential_id),
        )
        connection.commit()
        return _result("success")
    except Exception as error:
        return _result("error", error=str(error))


def assign_category(credential_ids, category: str):
    try:
        connection = _ensure_connection()
        connection.executemany(
            "UPDATE vault_items SET category=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
            [(category, credential_id) for credential_id in credential_ids],
        )
        connection.commit()
        return _result("success")
    except Exception as error:
        return _result("error", error=str(error))


def category_summary():
    try:
        rows = _ensure_connection().execute(
            "SELECT category, COUNT(*) AS count FROM vault_items GROUP BY category ORDER BY category COLLATE NOCASE"
        ).fetchall()
        return _result("success", data={row["category"] or "Other": row["count"] for row in rows})
    except Exception as error:
        return _result("error", error=str(error), data={})
