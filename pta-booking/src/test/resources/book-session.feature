Feature: To book one session

  Rule: A participant with one subscription can book a session

    Scenario: One participant books one private session
      Given one participant p with one yearly subscription su
      And one private session ps
      When p books ps using su
      Then ps has one booking for p with status DONE

    Scenario: One participant books one small group session
      Given one participant p with one yearly subscription su
      And one small group session ps
      When p books ps using su
      Then ps has one booking for p with status PREBOOKED

    Scenario: Two participants book one small group session
      Given one participant p1 with one yearly subscription su1
      And one participant p2 with one yearly subscription su2
      And one small group session ps
      When p1 books ps using su1
      And p2 books ps using su2
      Then ps has one booking for p1 with status DONE
      And ps has one booking for p2 with status DONE

    Scenario: Two participants book one private session
      Given one participant p1 with one yearly subscription su1
      And one participant p2 with one yearly subscription su2
      And one private session ps
      When p1 books ps using su1
      And p2 books ps using su2
      Then ps has one booking for p1 with status DONE
      And ps has one booking for p2 with status WAITING

    Scenario: First of two participants cancels his booking
      Given one participant p1 with one yearly subscription su1
      And one participant p2 with one yearly subscription su2
      And one private session ps
      When p1 books ps using su1
      And p2 books ps using su2
      And p1 cancels his booking for ps
      Then ps has no booking for p1
      And ps has one booking for p2 with status DONE

