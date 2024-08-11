## To compile program

- In the root directory of the project, run the following command to compile the program:

```bash
mkdir -p out/production/cybrassignment
javac -d out/production/cybrassignment src/part4/*.java
```

## Part 4 - Brute-Force Attack

Types:
0: password is at most 6 characters long, composed only of lowercase letters.
1: password is at most 6 characters long, composed only of lowercase letters and numbers.
2: password is at most 6 characters long, composed only of lowercase and uppercase letters.

**Encrypting the file (using part 2):**
```bash
cd out/production/cybrassignment
echo "Hello, this is a test file!!!" > src/part4/plaintext.txt
java -cp . part2.Part2 enc -i src/part4/plaintext.txt --pass "apple" -o src/part4/plaintext.txt.enc
```

**Running the bruteforce:**
```bash
cd out/production/cybrassignment
java -cp . part4.Part4 src/part4/plaintext.txt.enc --type 0
java -cp . part4.Part4 src/part4/plaintext.txt.enc --type 1
java -cp . part4.Part4 src/part4/plaintext.txt.enc --type 2
```

Times recorded for cracking the ciphertext (using password 'apple') using brute-force with different character sets:

- **Type 0** (password composed only of lowercase letters):
    - Time taken: X milliseconds

- **Type 1** (password composed of lowercase letters and numbers):
    - Time taken: Y milliseconds

- **Type 2** (password composed of lowercase and uppercase letters):
    - Time taken: Z milliseconds
