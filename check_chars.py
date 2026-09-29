import sqlite3
db = sqlite3.connect('app/src/main/assets/adr_2025.db')
cursor = db.cursor()
cursor.execute("SELECT nombre FROM adr_tabla_a WHERE nombre LIKE '%AM%' AND nombre LIKE '%NICO%' LIMIT 1")
row = cursor.fetchone()
if row:
    nombre = row[0]
    print('Nombre:', repr(nombre))
    for i, c in enumerate(nombre):
        print(f'  [{i}] U+{ord(c):04X} = {c!r}')
db.close()