## To compile program

- In the root directory of the project, run the following command to compile the program:

```bash
mkdir -p out/production/cybrassignment
javac -d out/production/cybrassignment src/part1/*.java
```

## Part 1 - Perform symmetric encryption and decryption

**Encryption:**
```bash
cd out/production/cybrassignment
echo "Hello, this is a test file!!!" > part1/plaintext.txt
java -cp . part1.Part1 enc -i part1/plaintext.txt -m CBC
```

**Decryption:**
```bash
cd out/production/cybrassignment
java -cp . part1.Part1 dec -i part1/plaintext.txt.enc -k part1/key.base64 -iv part1/iv.base64 -m CBC
```
