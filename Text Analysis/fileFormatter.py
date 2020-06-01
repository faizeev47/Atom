with open('The Mothership - questions.txt', encoding='utf-8-sig') as f:
    nPages = int(f.readline())
    for page in range(1, nPages + 1):
        print("<string name=\"page" + str(page) + "_questions\">")
        pageNo, nQuestions  = (int(c) for c in f.readline().strip().split(' '))
        for question in range(1, nQuestions + 1):
            line = f.readline().strip() + ">"
            for i in range(3):
                parsed = f.readline().strip()
                if (i == 0):
                    parsed = parsed[1:]
                line += parsed
            line += ">" + f.readline().strip()
            print(line)
        print("</string>")

            