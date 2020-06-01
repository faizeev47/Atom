with open('mothership_questions.in', encoding='utf-8-sig') as f:
    pages = int(f.readline())
    for page in range(1, pages + 1):
        page_no, questions = (int(x) for x in f.readline().strip().split(' '))
        print("Page # {}".format(page_no))
        for question_no in range(1, questions + 1):
            tag, question = (s for s in f.readline().strip().split('>'))
            choices = list()
            for i in range(3):
                choices.append(f.readline().strip().split(":")[1])
            print("Q{}. {}\ttag: {}\nChoices:\n\t1. {}\n\t2. {}\n\t3. {}".format(
                    question_no,
                    question,
                    tag,
                    choices[0],
                    choices[1],
                    choices[2]))