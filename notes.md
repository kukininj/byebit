


https://github.com/LFDT-web3j/web3j
https://docs.web3j.io/4.11.0/


https://chainlist.org/chain/17000?testnets=true

https://holesky.etherscan.io/


# Model bezpieczeństwa danych

## Pliki
Aplikacja zapisuje na dysku pliki portfele utworzone przy pomocy biblioteki web3j (`WalletUtils.generateNewWalletFile`).
Struktura pliku portfela:
```json
{
  "address": "61505d33018d68108c485ba5e545a7f47437207c",
  "id": "059f3fe5-5991-4da1-a64c-4787d4ca91ef",
  "version": 3,
  "crypto": {
    "cipher": "aes-128-ctr",
    "ciphertext": "9d20a9bc168080706c7ee22e64c4bb2b62e692f320d2a99738b1e7299cf6958a",
    "cipherparams": {
      "iv": "32df36776774b4c921427611ddb86012"
    },
    "kdf": "scrypt",
    "kdfparams": {
      "dklen": 32,
      "n": 262144,
      "p": 1,
      "r": 8,
      "salt": "17e8580f36545c5b32f347ea0d8c6e3125fbe34dbc126216dd7e664d9b79ee5c"
    },
    "mac": "b6b0d53f38949c18e107d09aa1a6750f2416372a4caf50c3b40cd5b862e7bed7"
  }
}
```
Plik zawiera najważniejsze informacje o portfelu oraz dane zastosowane do szyfrowania klucza prywatnego.
Szyfrowanie klucza prywatnego znajduje się po stronie biblioteki web3j, 
zastosowany został algorytm aes-128-ctr, czyli jest to szyfr symetryczny - to samo hasło do zapisu i odczytu.
Hasło do klucza jest generowane na podstawie hasła podanego przez użytkownika, 
do generowania hasła biblioteka stosuje algorytm scrypt 
który odpowiada za generowanie klucza o odpowiedniej długości dla algorytmu AES.

## KeyStore
W keystore zapisywane są klucze podane przez użytkownika, które potem są wykorzystywane do odczytu klucza prywatnego z plików.

## Export danych
Export danych polega na spakowaniu zapisanych plików portfeli do archiwum ZIP, zaszyfrowanego podanym hasłem.


