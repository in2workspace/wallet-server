# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.0.0](https://github.com/in2workspace/wallet-server/releases/tag/v1.0.0) - 2024-2-19
### Added
- Implemented key pair generation using the secp256r1 algorithm.
- Generate did:key identifier from ES256 key algorithm.
- Generate did:key:jwk_jcs-pub identifier from ES256 key algorithm.
- Support Scorpio Context Broker (v.4.1.14) 
- Support Orion-LD
- Support Hashicorp Vault
- Support AZ Key Vault
- Introduced EBSI-compliant flows for issuing and presenting digital credentials.
- Developed functionalities for QR content interpretation and credential offer management.
- Enabled the ability to interpret presentation submission filters for credential verification against user criteria.
- Creational Patterns, utilization of the Builder pattern
- Structural Patterns, implementation of the Facade pattern.
- Factory and Adapter Patterns: Current use of the Factory pattern combined with the Adapter pattern, enhancing the flexibility and reusability of object creation and interaction within the system.
- Type Interpretation in Presentation Definition Filters: Added the capability to interpret the type of Verifiable Credential in Presentation Definition filters, allowing for more precise selection and verification of the credentials presented by the user. This enhancement facilitates adaptation to various use cases where verifying the specific type of the user's verifiable credential is required.

## [v1.1.0](https://github.com/in2workspace/wallet-server/releases/tag/v1.1.0) - 2024-3-26
### Added
- Support Verifiable Credentials in cwt format
- Introduced the authentication process for Verifiable Presentations flow [OpenID.VP] combined with the Self-Issued OP v2 specification [OpenID.SIOP2]
- Introduced the Verifiable Presentation flow for CWT (Cbor web token) format
- Introduced the abstraction of the configuration and the vault
- Compatibility with the attestation exchange of the DOME marketplace. 

## [v1.1.0](https://github.com/in2workspace/wallet-server/releases/tag/v1.1.1) - 2024-3-26
### Fixed
- Change the versioning of the api endpoints from v2 to v1