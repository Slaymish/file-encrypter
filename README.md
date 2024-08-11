# CYBR372 Assignment 1


## To compile program

- In the root directory of the project, run the following command to compile the program:

```bash
mkdir -p out/production/cybrassignment
javac -d out/production/cybrassignment src/main/java/part1/*.java src/main/java/part2/*.java src/main/java/part3/*.java src/main/java/part4/*.java
```

## Part 1 - Perform symmetric encryption and decryption

**Encryption:**
```bash
cd out/production/cybrassignment
echo "Hello, this is a test file!!!" > main/java/part1/plaintext.txt
java -cp . main.java.part1.Part1 enc -i main/java/part1/plaintext.txt -m CBC
```

**Decryption:**
```bash
cd out/production/cybrassignment
java -cp . main.java.part1.Part1 dec -i main/java/part1/plaintext.txt.enc -k main/java/part1/key.base64 -iv main/java/part1/iv.base64 -m CBC
```


## Part 2 - Password-Based Key Derivation for encryption/decryption

**Encryption:**
```bash
cd out/production/cybrassignment
echo "Hello, this is a test file!!!" > main/java/part2/plaintext.txt    
java -cp . main.java.part2.Part2 enc -i main/java/part2/plaintext.txt --pass "password123" -o main/java/part2/plaintext.txt.enc
```

**Decryption:**
```bash
cd out/production/cybrassignment
java -cp . main.java.part2.Part2 dec -i main/java/part2/plaintext.txt.enc --pass "password123" -o main/java/part2/plaintext.txt.dec
```


## Part 3 - Evaluation of Parameters on Performance

The CSV file contains the following columns:

- Mode: The mode of operation used for encryption.
- Key Size (bytes): The size of the encryption key in bytes (16 bytes = 128 bits, 24 bytes = 192 bits, etc.).
- File Size (KB): The size of the plaintext file in kilobytes.
- ENC Time (ns): The time taken to encrypt the file, measured in nanoseconds.
- DEC Time (ns): The time taken to decrypt the file, measured in nanoseconds (some entries have NaN, meaning decryption time was not measured or recorded).



## Part 4 - Brute-Force Attack