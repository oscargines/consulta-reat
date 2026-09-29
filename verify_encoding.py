import sqlite3
db = sqlite3.connect('app/src/main/assets/adr_2025.db')
cursor = db.cursor()
cursor.execute("SELECT COUNT(*) FROM adr_tabla_a WHERE nombre LIKE '%' || char(0xFFFD) || '%'")
print('Tabla A with replacement char:', cursor.fetchone()[0])
cursor.execute("SELECT COUNT(*) FROM adr_tabla_b WHERE nombre LIKE '%' || char(0xFFFD) || '%'")
print('Tabla B with replacement char:', cursor.fetchone()[0])

# Check specific rows
cursor.execute("SELECT nombre FROM adr_tabla_a WHERE nombre LIKE '%AM%' AND nombre LIKE '%NICO%' LIMIT 3")
print('AMONICO rows:', cursor.fetchall())

cursor.execute("SELECT nombre FROM adr_tabla_b WHERE nombre LIKE '%AM%' AND nombre LIKE '%NICO%' LIMIT 3")
print('Tabla B AMONICO:', cursor.fetchall())

db.close()