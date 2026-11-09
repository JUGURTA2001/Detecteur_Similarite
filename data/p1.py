# p1.py
def table(n, maxi=10):
    lignes = []
    for i in range(1, maxi + 1):
        lignes.append(f"{n} x {i} = {n * i}")
    return lignes

if __name__ == "__main__":
    for ligne in table(7):
        print(ligne)