def plus_grand(valeurs):
    best = valeurs[0]
    k = 1
    while k < len(valeurs):
        if best < valeurs[k]:
            best = valeurs[k]
        k += 1
    return best