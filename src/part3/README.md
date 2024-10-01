## To compile program

- In the root directory of the project, run the following command to compile the program:

```bash
mkdir -p out/production/cybrassignment
javac -d out/production/cybrassignment src/part3/*.java
```


## Part 3 - Evaluation of Parameters on Performance

**CSV Columns:**
Mode: CBC or ECB, etc.
Key Size (bits): 128, 192, 256, etc.
File Size (KB): 10, 100, 1000, etc.
Encryption Time (ns): Time taken to encrypt, averaged over runs.
Decryption Time (ns): Time taken to decrypt, averaged over runs.


```bash
cd out/production/cybrassignment
java -cp . part3.Part3 -o part3/results.csv -repeats 100
```