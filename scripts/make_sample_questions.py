#!/usr/bin/env python3
"""Generate 25 original AMC 8-style problems; these are NOT copied AMC problems."""
import json
from pathlib import Path

# Fields: prompt, choices, correct (zero-based A..E), hint, two worked explanations.
items = [
("A rectangle has perimeter 30 cm and width 6 cm. What is its length?", ["6 cm", "7 cm", "8 cm", "9 cm", "10 cm"], 3, "The perimeter is twice the sum of length and width.", "Let the length be L. Then 2(L + 6) = 30, so L + 6 = 15 and L = 9 cm.", "Half the perimeter is 15 cm. Take away the 6 cm width: the length is 9 cm."),
("A bag holds 5 red, 7 blue, and 8 green marbles. What is the probability of picking a blue marble?", ["1/4", "7/20", "2/5", "7/13", "1/3"], 1, "Count all the marbles first.", "There are 5 + 7 + 8 = 20 marbles, of which 7 are blue. Probability = 7/20.", "The favorable outcomes are the 7 blue marbles; total equally likely outcomes are 20. Therefore 7/20."),
("The sequence begins 3, 7, 11, 15, ... What is its 10th term?", ["35", "37", "39", "41", "43"], 2, "Each new term is 4 larger than the last.", "The nth term is 3 + 4(n - 1). For n = 10, this equals 3 + 36 = 39.", "The 10th term is nine jumps after the first. Nine jumps of 4 total 36; 3 + 36 = 39."),
("The mean of 4, 8, 10, and x is 9. What is x?", ["12", "13", "14", "15", "16"], 2, "The sum of four numbers with mean 9 is 36.", "4 + 8 + 10 + x = 4 × 9 = 36. Therefore x = 36 - 22 = 14.", "The existing three values add to 22. The target total is 36, so the missing amount is 14."),
("Three-fifths of a number equals 24. What is that number?", ["30", "36", "40", "45", "48"], 2, "First find one-fifth of the number.", "If (3/5)n = 24, then n = 24 × 5/3 = 40.", "Three equal fifths make 24, so one fifth is 8. Five fifths make 5 × 8 = 40."),
("Two angles of a triangle measure 45° and 65°. What is its third angle?", ["60°", "65°", "70°", "75°", "80°"], 2, "The interior angles of a triangle sum to 180°.", "Third angle = 180° - 45° - 65° = 70°.", "The two known angles total 110°. A straight angle is 180°, leaving 70°."),
("Two students share 120 stickers in the ratio 3:5. How many does the student with the larger share receive?", ["45", "60", "72", "75", "80"], 3, "The ratio has eight total parts.", "There are 3 + 5 = 8 equal parts. Each part is 120/8 = 15, so the larger share is 5 × 15 = 75.", "The larger share is 5/8 of the whole: 120 × 5/8 = 75."),
("A square has area 81 square centimeters. What is its perimeter?", ["18 cm", "27 cm", "32 cm", "36 cm", "40 cm"], 3, "A square's area is side × side.", "Its side length is sqrt(81) = 9 cm. Its perimeter is 4 × 9 = 36 cm.", "A 9-by-9 square has area 81. Four equal 9-cm sides total 36 cm."),
("How many two-digit positive integers are multiples of 5?", ["16", "17", "18", "19", "20"], 2, "Start at 10 and end at 95.", "The multiples are 10, 15, ..., 95. Count = (95 - 10)/5 + 1 = 18.", "There are 19 multiples of 5 at most 95, counting 5. Remove the one-digit number 5 to get 18."),
("A jacket costs $80. It is discounted 25%, then a 10% tax is charged on the discounted price. What is the final cost?", ["$60", "$62", "$64", "$66", "$70"], 3, "Apply the discount before calculating the tax.", "Discounted price: 80 × 0.75 = 60. Tax: 60 × 0.10 = 6. Total: $66.", "Multiply the original price by 0.75 × 1.10 = 0.825. Thus 80 × 0.825 = $66."),
("How many pairs of positive integers (x, y) satisfy x + y = 10 and x > y?", ["2", "3", "4", "5", "6"], 2, "List the smaller positive value y.", "Possible pairs are (9,1), (8,2), (7,3), (6,4), for a total of four.", "Since x > y and x + y = 10, y can be 1, 2, 3, or 4. Thus there are four pairs."),
("What is the smaller angle between the hands of a clock at exactly 3:00?", ["30°", "60°", "90°", "120°", "150°"], 2, "A clock face has 12 equally spaced hour marks.", "At 3:00 the minute hand points to 12 and the hour hand to 3. Three hour spacings of 30° give 90°.", "A quarter turn of a circle is 360°/4 = 90°. The hands are one quarter turn apart."),
("Five consecutive integers have a sum of 85. What is the largest integer?", ["17", "18", "19", "20", "21"], 2, "The middle integer is also their average.", "The mean is 85/5 = 17, so the integers are 15, 16, 17, 18, 19. Largest is 19.", "Let the middle number be n. The sum is 5n = 85, so n = 17; add 2 for the largest: 19."),
("How many squares of any size can be found in a 3-by-3 grid of unit squares?", ["9", "12", "13", "14", "15"], 3, "Count 1-by-1, then 2-by-2, then 3-by-3 squares.", "There are 9 unit squares, 4 squares of side 2, and 1 square of side 3. Total = 14.", "Use 1² + 2² + 3² = 1 + 4 + 9 = 14 squares."),
("Two fair six-sided dice are rolled. What is the probability that their sum is 7?", ["1/12", "1/9", "1/6", "1/4", "1/3"], 2, "There are 36 equally likely ordered outcomes.", "The six winning outcomes are (1,6),(2,5),(3,4),(4,3),(5,2),(6,1). So probability = 6/36 = 1/6.", "Each first-die value from 1 through 6 determines exactly one second-die value. That is 6 wins out of 36 outcomes, or 1/6."),
("A cyclist rides at 12 km/h for 90 minutes. How far do they travel?", ["12 km", "15 km", "18 km", "20 km", "24 km"], 2, "Ninety minutes is one and a half hours.", "Distance = speed × time = 12 × (90/60) = 18 km.", "In one hour, travel 12 km; in a further half hour, 6 km. Total 18 km."),
("A box contains red and blue blocks in the ratio 2:3. After adding 5 red blocks, the colors have equal counts. How many blocks were originally in the box?", ["15", "20", "25", "30", "35"], 2, "The difference between the red and blue counts is one ratio part.", "Originally red = 2k and blue = 3k. Since 2k + 5 = 3k, k = 5; total = 5k = 25.", "Five added red blocks exactly fill the missing one ratio part. Each part is 5, and there are five parts total, giving 25."),
("How many integers from 1 through 100 are divisible by both 4 and 6?", ["6", "7", "8", "9", "10"], 2, "A number divisible by both must be divisible by their least common multiple.", "lcm(4,6) = 12. Multiples of 12 up to 100 are 12,24,...,96, giving floor(100/12) = 8.", "12 × 8 = 96 but 12 × 9 = 108 > 100. Exactly eight positive multiples fit."),
("A rectangle is 8 units long and 9 units wide. What is its area?", ["64", "68", "70", "72", "80"], 3, "Area of rectangle = length × width.", "Multiply 8 × 9 to get 72 square units.", "Divide the rectangle into eight strips of nine unit squares. There are 8 × 9 = 72 unit squares."),
("Three-eighths of a class of 24 students play violin. How many students do not play violin?", ["12", "13", "14", "15", "16"], 3, "First count the students who do play violin.", "There are (3/8) × 24 = 9 violin players. The other 24 - 9 = 15 do not play.", "The non-violin fraction is 1 - 3/8 = 5/8. So (5/8) × 24 = 15."),
("Each of four friends independently chooses one of three ice-cream flavors. How many ordered combinations of choices are possible?", ["27", "64", "72", "81", "96"], 3, "Each additional friend triples the count.", "There are 3 choices for each of 4 friends, giving 3 × 3 × 3 × 3 = 81.", "By the multiplication principle, 3^4 = 81 different assignments."),
("What is the sum of the interior angles of a hexagon?", ["540°", "600°", "680°", "720°", "900°"], 3, "A hexagon can be split into four triangles.", "Sum = (6 - 2) × 180° = 720°.", "Draw diagonals from one vertex to make 4 triangles. Each triangle contributes 180°, total 720°."),
("A train is 150 meters long and passes a stationary pole completely in 10 seconds. What is its speed?", ["10 m/s", "12 m/s", "15 m/s", "20 m/s", "25 m/s"], 2, "Passing a pole means the train moves its own length.", "Speed = distance/time = 150/10 = 15 m/s.", "In 10 seconds the train travels 150 meters. In one second it travels 15 meters."),
("Which of these positive integers has exactly three positive factors?", ["16", "27", "36", "49", "64"], 3, "A number has exactly three positive divisors when it is the square of a prime.", "49 = 7² has factors 1, 7, and 49, exactly three. Other choices have more.", "The prime-square rule gives exactly three divisors: 1, p, p². Among the choices 49 = 7² works."),
("If 2^a = 32 and 3^b = 81, what is a + b?", ["7", "8", "9", "10", "11"], 2, "Rewrite 32 and 81 as prime powers.", "32 = 2^5 so a = 5; 81 = 3^4 so b = 4. Therefore a + b = 9.", "Count powers: 2×2×2×2×2 is 32 and 3×3×3×3 is 81. Add their exponents, 5 + 4 = 9."),
]
assert len(items) == 25
questions = []
for i, (prompt, choices, correct, hint, s1, s2) in enumerate(items, 1):
    assert len(choices) == 5 and len(set(choices)) == 5 and 0 <= correct < 5
    questions.append({
        'id': f'ORIGINAL-MATHBREAK-{i:02d}', 'prompt': prompt,
        'choices': choices, 'correct': correct, 'hint': hint,
        'solutions': [s1, s2], 'videoUrl': '',
        'source': 'Original MathBreak demonstration content (not AMC 8 contest questions)'
    })
path = Path(__file__).resolve().parents[1] / 'app/src/main/assets/questions.json'
path.write_text(json.dumps(questions, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(f'Wrote {len(questions)} original questions to {path}')
