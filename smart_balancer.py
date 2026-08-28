with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

out = []
depth = 0

for line in lines:
    # count `{` and `}`
    # ignore those in strings
    in_string = False
    escape = False
    l_count = 0
    r_count = 0
    for char in line:
        if escape:
            escape = False
            continue
        if char == '\\':
            escape = True
        elif char == '"':
            in_string = not in_string
        elif not in_string:
            if char == '{':
                l_count += 1
            elif char == '}':
                r_count += 1
    
    # If we hit a top-level annotation or declaration and we have leftover depth, close them!
    # A top level declaration usually starts with exactly "@Composable" or "fun " or "class " at col 0.
    if line.startswith("@Composable") or line.startswith("@OptIn") or (line.startswith("fun ") and depth > 0):
        if depth > 0:
            # We are missing `depth` closing braces!
            print(f"Adding {depth} braces before {line.strip()}")
            while depth > 0:
                out.append("    " * (depth - 1) + "}\n")
                depth -= 1
    
    depth += (l_count - r_count)
    out.append(line)

while depth > 0:
    out.append("    " * (depth - 1) + "}\n")
    depth -= 1

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(out)

