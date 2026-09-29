import sqlite3
db = sqlite3.connect('app/src/main/assets/adr_2025.db')
print('A:', db.execute('PRAGMA table_info(adr_tabla_a)').fetchall())
print('B:', db.execute('PRAGMA table_info(adr_tabla_b)').fetchall())
print('Index:', db.execute("SELECT name FROM sqlite_master WHERE type='index'").fetchall())

# Test queries
cursor = db.cursor()
cursor.execute("SELECT nombre, nombre_normalized FROM adr_tabla_b WHERE numero_onu='1001' LIMIT 3")
print('ONU 1001:', cursor.fetchall())

cursor.execute("SELECT nombre, nombre_normalized FROM adr_tabla_b WHERE nombre_normalized LIKE '%gasolina%' LIMIT 3")
print('gasolina:', cursor.fetchall())

cursor.execute("SELECT nombre, nombre_normalized FROM adr_tabla_b WHERE nombre_normalized LIKE '%amoniaco%' LIMIT 3")
print('amoniaco:', cursor.fetchall())

db.close()