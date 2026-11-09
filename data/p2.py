# p2.py
def table(n, maxi=10):
    return [f"{n} x {i} = {n * i}" for i in range(1, maxi + 1)]

if __name__ == "__main__":
    for ligne in table(7):
        print(ligne)