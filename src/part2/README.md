## To compile program

- In the root directory of the project, run the following command to compile the program:

```bash
mkdir -p out/production/cybrassignment
javac -d out/production/cybrassignment src/part2/*.java
```

## Part 2 - Password-Based Key Derivation for encryption/decryption

Since the salt is stored in the encrypted file, there's no need for the user to provide it during decryption. The program automatically reads the salt and IV from the file, uses them to reconstruct the key and initialize the cipher, and then proceeds to decrypt the data. The salt is generated during encryption, used to derive the key, and stored in the encrypted file.


**Encryption:**
```bash
cd out/production/cybrassignment
echo "Hello, this is a test file!!!" > src/part2/plaintext.txt    
java -cp . part2.Part2 enc -i src/part2/plaintext.txt --pass "password123" -o src/part2/plaintext.txt.enc
```

**Decryption:**
```bash
cd out/production/cybrassignment
java -cp . part2.Part2 dec -i src/part2/plaintext.txt.enc --pass "password123" -o src/part2/plaintext.txt.dec
```