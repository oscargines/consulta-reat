import sqlite3
db = sqlite3.connect('app/src/main/assets/adr_2025.db')
cursor = db.cursor()
cursor.execute("SELECT numero_onu, nombre, clase, etiquetas FROM adr_tabla_a WHERE etiquetas IS NOT NULL AND etiquetas != '' LIMIT 20")
for row in cursor.fetchall():
    print(f"ONU {row[0]} | Clase {row[2]} | Etiquetas: {row[3]} | {row[1][:60]}")
db.close()