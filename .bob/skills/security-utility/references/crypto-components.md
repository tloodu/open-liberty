# Crypto Component Dependencies

The security utility integrates with several crypto components for cryptographic operations. These components are separate OSGi bundles that provide specialized functionality.

## Overview

The security utility depends on these crypto components:
- `com.ibm.ws.crypto.certificateutil` - SSL certificate creation
- `com.ibm.ws.crypto.ltpakeyutil` - LTPA key generation
- `com.ibm.ws.crypto.passwordutil` - Password encoding/decoding
- `com.ibm.ws.crypto.aeskeyutil` - AES key management

## com.ibm.ws.crypto.certificateutil

Provides functionality for creating self-signed SSL certificates and keystores. Used by the `createSSLCertificate` command to generate certificates for securing Liberty server communications.

### Purpose
Creates self-signed SSL certificates and keystores for Liberty servers.

### Key Classes
- `CertificateCreator` - Main class for certificate creation
- `DefaultSSLCertificateCreator` - Default implementation

### Usage Pattern

```java
import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;

// Create certificate creator
DefaultSSLCertificateCreator creator = new DefaultSSLCertificateCreator(
    keystorePath,           // Path to keystore file
    keystorePassword,       // Keystore password
    validityDays,          // Certificate validity in days
    subjectDN,             // Subject DN (e.g., "CN=localhost,O=IBM,C=US")
    keySize,               // Key size (2048, 4096)
    signatureAlgorithm     // Algorithm (e.g., "SHA256WithRSA")
);

// Create the certificate
creator.createCertificate();
```

### Common Parameters
- **keystorePath**: Absolute path to output keystore file
- **keystorePassword**: Password for keystore (min 6 characters)
- **validityDays**: Certificate validity period (default: 365)
- **subjectDN**: Distinguished name for certificate subject
- **keySize**: RSA key size (2048 or 4096 recommended)
- **signatureAlgorithm**: Signature algorithm (SHA256WithRSA, SHA384WithRSA, SHA512WithRSA)

### Error Handling

```java
try {
    creator.createCertificate();
} catch (CertificateException e) {
    stderr.println(getMessage("error.certificateCreation", e.getMessage()));
    return SecurityUtilityReturnCodes.ERR_GENERIC;
}
```

## com.ibm.ws.crypto.ltpakeyutil

Provides LTPA key generation capabilities for single sign-on (SSO) between Liberty servers. Used by the `createLTPAKeys` command to generate shared authentication keys.

### Purpose
Generates LTPA (Lightweight Third Party Authentication) keys for SSO.

### Key Classes
- `LTPAKeyCreator` - Main class for LTPA key generation
- `LTPAKeyUtil` - Utility methods for LTPA keys

### Usage Pattern

```java
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyCreator;

// Create LTPA key creator
LTPAKeyCreator creator = new LTPAKeyCreator(
    outputFile,            // Path to output .keys file
    password,              // Password for key encryption
    validityDays           // Key validity in days (optional)
);

// Generate the keys
creator.createLTPAKeys();
```

### Output Format

The generated `.keys` file contains three properties:
```properties
com.ibm.websphere.ltpa.version=1.0
com.ibm.websphere.ltpa.PrivateKey=<base64-encoded-private-key>
com.ibm.websphere.ltpa.PublicKey=<base64-encoded-public-key>
com.ibm.websphere.ltpa.Realm=<realm-name>
com.ibm.websphere.CreationDate=<timestamp>
com.ibm.websphere.CreationHost=<hostname>
```

### Common Parameters
- **outputFile**: Path to output `.keys` file
- **password**: Password for encrypting private key
- **validityDays**: Key validity period (default: 365)

### Error Handling

```java
try {
    creator.createLTPAKeys();
} catch (Exception e) {
    stderr.println(getMessage("error.ltpaKeyCreation", e.getMessage()));
    return SecurityUtilityReturnCodes.ERR_GENERIC;
}
```

## com.ibm.ws.crypto.passwordutil

Provides password encoding and decoding functionality with multiple encryption schemes. Used by the `encode` command to secure passwords in server configuration files.

### Purpose
Encodes and decodes passwords using various encryption schemes.

### Key Classes
- `PasswordUtil` - Main utility class
- `InvalidPasswordEncodingException` - Thrown for invalid encoding
- `InvalidPasswordDecodingException` - Thrown for invalid decoding
- `UnsupportedCryptoAlgorithmException` - Thrown for unsupported algorithms

### Supported Encoding Schemes
- **xor**: Simple XOR encoding (reversible, not secure)
- **aes**: AES encryption (reversible, secure)
- **hash**: One-way hash (not reversible, secure)
- **custom**: Custom encryption (requires custom implementation)

### Usage Pattern

#### Encoding Passwords

```java
import com.ibm.ws.crypto.passwordutil.PasswordUtil;

// XOR encoding (default)
String encoded = PasswordUtil.encode(password, "xor", null);
// Returns: {xor}CDo9Hgw=

// AES encoding (requires encryption key)
Properties props = new Properties();
props.setProperty("wlp.password.encryption.key", aesKey);
String encoded = PasswordUtil.encode(password, "aes", props);
// Returns: {aes}AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=

// Hash encoding (one-way)
String encoded = PasswordUtil.encode(password, "hash", null);
// Returns: {hash}ATAAAA...
```

#### Decoding Passwords

```java
// Decode XOR-encoded password
String decoded = PasswordUtil.decode(encodedPassword);

// Decode AES-encoded password (requires same key)
Properties props = new Properties();
props.setProperty("wlp.password.encryption.key", aesKey);
String decoded = PasswordUtil.decode(encodedPassword, props);
```

### AES Key Management

AES encoding requires an encryption key. The key can be:
1. Generated using `generateAesKey` command
2. Provided via `--key` argument (base64-encoded)
3. Read from server configuration

```java
// Generate AES key
String aesKey = AESKeyManager.generateKey(256); // 128 or 256 bits

// Use key for encoding
Properties props = new Properties();
props.setProperty("wlp.password.encryption.key", aesKey);
String encoded = PasswordUtil.encode(password, "aes", props);
```

### Error Handling

```java
try {
    String encoded = PasswordUtil.encode(password, encoding, props);
} catch (InvalidPasswordEncodingException e) {
    stderr.println(getMessage("error.invalidEncoding", encoding));
    return SecurityUtilityReturnCodes.ERR_GENERIC;
} catch (UnsupportedCryptoAlgorithmException e) {
    stderr.println(getMessage("error.unsupportedAlgorithm", encoding));
    return SecurityUtilityReturnCodes.ERR_GENERIC;
}
```

## Crypto Component Integration Patterns

Common patterns for integrating crypto components into security utility tasks. These patterns demonstrate proper error handling, parameter validation, and component usage.

### Pattern 1: Password Encoding with PasswordUtil

**Use Case**: Encoding passwords for server configuration files with support for multiple encryption schemes (xor, aes, hash).

**Key Differences**:
- Handles multiple encoding schemes with conditional logic
- Requires Properties object for AES encryption key
- Returns encoded string directly to stdout
- No file creation involved

```java
// In EncodeTask.java
String password = getArgumentValue(ARG_PASSWORD, args, null);
String encoding = getArgumentValue(ARG_PASSWORD_ENCODING, args, "xor");

Properties props = new Properties();
if ("aes".equals(encoding)) {
    String key = getArgumentValue(ARG_KEY, args, null);
    if (key != null) {
        props.setProperty("wlp.password.encryption.key", key);
    }
}

try {
    String encoded = PasswordUtil.encode(password, encoding, props);
    stdout.println(encoded);
    return SecurityUtilityReturnCodes.OK;
} catch (Exception e) {
    stderr.println(getMessage("error.encoding", e.getMessage()));
    return SecurityUtilityReturnCodes.ERR_GENERIC;
}
```

### Pattern 2: AES Key Management

**Use Case**: Generating AES encryption keys for use with password encoding.

**Key Differences**:
- Generates cryptographic key material (not encoding existing data)
- Supports both file output and stdout output
- Simpler parameter handling (only key size)
- No password prompting required

```java
// In GenerateAesKeyTask.java
int keySize = Integer.parseInt(getArgumentValue(ARG_SIZE, args, "256"));

try {
    String aesKey = AESKeyManager.generateKey(keySize);
    
    // Write to file or output
    if (outputFile != null) {
        Files.write(Paths.get(outputFile), aesKey.getBytes());
    } else {
        stdout.println(aesKey);
    }
    
    return SecurityUtilityReturnCodes.OK;
} catch (Exception e) {
    stderr.println(getMessage("error.keyGeneration", e.getMessage()));
    return SecurityUtilityReturnCodes.ERR_GENERIC;
}
```

### Pattern 3: SSL Certificate Creation

**Use Case**: Creating self-signed SSL certificates and keystores for securing server communications.

**Key Differences**:
- Creates binary keystore file (not text output)
- Requires password prompting for keystore protection
- Multiple configuration parameters (validity, subject DN, key size, algorithm)
- Uses creator object pattern with multiple constructor parameters
- Catches specific CertificateException

```java
// In CreateSSLCertificateTask.java
String keystorePath = resolveOutputPath(args);
String password = getPasswordFromArgs(stdin, stdout, args);
int validityDays = Integer.parseInt(getArgumentValue(ARG_VALIDITY, args, "365"));
String subjectDN = getArgumentValue(ARG_SUBJECT, args, "CN=localhost,O=IBM,C=US");

try {
    DefaultSSLCertificateCreator creator = new DefaultSSLCertificateCreator(
        keystorePath,
        password,
        validityDays,
        subjectDN,
        2048,
        "SHA256WithRSA"
    );
    
    creator.createCertificate();
    stdout.println(getMessage("certificate.created", keystorePath));
    return SecurityUtilityReturnCodes.OK;
} catch (CertificateException e) {
    stderr.println(getMessage("error.certificateCreation", e.getMessage()));
    return SecurityUtilityReturnCodes.ERR_GENERIC;
}
```

### Pattern 4: LTPA Key Generation

**Use Case**: Generating LTPA keys for single sign-on between Liberty servers.

**Key Differences**:
- Creates properties file with multiple key entries
- Requires password for key encryption
- Simpler parameter set than SSL certificates
- Output is text-based properties file
- Used for authentication/SSO rather than transport security

```java
// In CreateLTPAKeysTask.java
String outputPath = resolveOutputPath(args);
String password = getPasswordFromArgs(stdin, stdout, args);
int validityDays = Integer.parseInt(getArgumentValue(ARG_VALIDITY, args, "365"));

try {
    LTPAKeyCreator creator = new LTPAKeyCreator(
        outputPath,
        password,
        validityDays
    );
    
    creator.createLTPAKeys();
    stdout.println(getMessage("ltpaKeys.created", outputPath));
    return SecurityUtilityReturnCodes.OK;
} catch (Exception e) {
    stderr.println(getMessage("error.ltpaKeyCreation", e.getMessage()));
    return SecurityUtilityReturnCodes.ERR_GENERIC;
}
```

## Crypto Component Error Handling

Comprehensive error handling strategies for crypto operations. Crypto components throw specific exceptions that should be caught and converted to appropriate return codes with user-friendly error messages.

### Common Exceptions

```java
// Certificate creation errors
catch (CertificateException e) {
    // Invalid certificate parameters or creation failure
}

// Password encoding errors
catch (InvalidPasswordEncodingException e) {
    // Invalid encoding scheme or parameters
}

// Password decoding errors
catch (InvalidPasswordDecodingException e) {
    // Cannot decode password (wrong key, corrupted data)
}

// Unsupported algorithm errors
catch (UnsupportedCryptoAlgorithmException e) {
    // Requested algorithm not available
}

// Key specification errors
catch (InvalidKeySpecException e) {
    // Invalid AES key specification
}

// Algorithm not available
catch (NoSuchAlgorithmException e) {
    // Cryptographic algorithm not available in JVM
}
```

### Error Handling Best Practices

1. **Catch Specific Exceptions**: Catch specific crypto exceptions before generic Exception
2. **Provide Context**: Include relevant details in error messages
3. **Return Appropriate Codes**: Use correct SecurityUtilityReturnCodes
4. **Log Stack Traces**: Print stack traces to stderr for debugging
5. **Validate Input**: Validate parameters before calling crypto components

```java
try {
    // Crypto operation
    return SecurityUtilityReturnCodes.OK;
} catch (InvalidPasswordEncodingException e) {
    stderr.println(getMessage("error.invalidEncoding", encoding, e.getMessage()));
    return SecurityUtilityReturnCodes.ERR_GENERIC;
} catch (UnsupportedCryptoAlgorithmException e) {
    stderr.println(getMessage("error.unsupportedAlgorithm", encoding));
    return SecurityUtilityReturnCodes.ERR_GENERIC;
} catch (Exception e) {
    stderr.println(getMessage("error.generic", e.toString()));
    e.printStackTrace(stderr);
    return SecurityUtilityReturnCodes.ERR_GENERIC;
}
```

## Crypto Component Import Statements

Required import statements for each crypto component. Include these imports when implementing tasks that use crypto functionality.

### Required Imports for Certificate Creation

```java
import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import java.security.cert.CertificateException;
```

### Required Imports for LTPA Keys

```java
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyCreator;
```

### Required Imports for Password Encoding

```java
import com.ibm.ws.crypto.passwordutil.PasswordUtil;
import com.ibm.ws.crypto.passwordutil.InvalidPasswordEncodingException;
import com.ibm.ws.crypto.passwordutil.InvalidPasswordDecodingException;
import com.ibm.ws.crypto.passwordutil.UnsupportedCryptoAlgorithmException;
```

### Required Imports for AES Keys

```java
import com.ibm.ws.crypto.aeskeyutil.AESKeyManager;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;
```

## Testing Considerations

Guidelines for testing crypto component integration. Security utility tests mock crypto components to enable fast, isolated unit testing without requiring actual cryptographic operations.

- Crypto components have their own unit tests
- Security utility tests mock crypto component interactions
- Test with various key sizes, algorithms, and error conditions
- Verify FIPS 140-3 compliance when applicable

## FIPS 140-3 Compliance

Requirements and considerations for FIPS 140-3 compliance. When FIPS mode is enabled, only approved algorithms and key sizes can be used.

When FIPS mode is enabled:
- Use FIPS-approved algorithms only
- Minimum key sizes: RSA 2048-bit, AES 128-bit
- Approved signature algorithms: SHA256WithRSA, SHA384WithRSA, SHA512WithRSA
- Validate crypto provider supports FIPS mode
- Test with FIPS-enabled JVM

```java
// Check if FIPS mode is enabled
if (isFIPSEnabled()) {
    // Use FIPS-approved algorithms only
    signatureAlgorithm = "SHA256WithRSA";
    keySize = Math.max(keySize, 2048);
}