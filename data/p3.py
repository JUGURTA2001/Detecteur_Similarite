# p3.py
def table(n, maxi=10):
    lignes = []
    i = 1
    while i <= maxi:
        lignes.append(f"{n} x {i} = {n * i}")
        i += 1
    return lignes

if __name__ == "__main__":
    for ligne in table(7):
        print(ligne)