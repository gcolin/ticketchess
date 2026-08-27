import secrets

jwt_key = secrets.token_hex(32)

print("JWT Key:")
print(jwt_key)