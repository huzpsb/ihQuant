import requests
import zlib
import time


class Item:
    def __init__(self, open_, close, high, low, volume, amount):
        self.open = open_
        self.close = close
        self.high = high
        self.low = low
        self.volume = volume
        self.amount = amount

    def __repr__(self):
        return (f"Item(open={self.open}, close={self.close}, high={self.high}, "
                f"low={self.low}, volume={self.volume}, amount={self.amount})")


def decrypt(encrypted_bytes: bytes) -> bytes:
    predefined_xor = b"ihq_exp_hello"
    decrypted = bytearray(len(encrypted_bytes))
    for i, b in enumerate(encrypted_bytes):
        b = (~b) & 0xFF
        decrypted[i] = b ^ predefined_xor[i % len(predefined_xor)]
    return bytes(decrypted)


def get_if_legend(part: str) -> str:
    if len(part) != 12:
        return None
    a, b = part[:6], part[6:]
    if a != b:
        raise IOError(f"Invalid legend part: {part}")
    return a


def main():
    start = time.time()
    url = "https://gh-proxy.com/https://github.com/huzpsb/ihq_bin/raw/refs/heads/main/ihq.bin"
    file_name = "ihq.bin"
    print("Downloading ihq.bin ...")
    with requests.get(url, stream=True) as r:
        r.raise_for_status()
        with open(file_name, "wb") as f:
            for chunk in r.iter_content(chunk_size=8192):
                f.write(chunk)
    with open(file_name, "rb") as f:
        encrypted_bytes = f.read()
    decrypted_bytes = decrypt(encrypted_bytes)
    decrypted_bytes = zlib.decompress(decrypted_bytes)
    content = decrypted_bytes.decode("utf-8")
    lines = content.splitlines()
    legends = lines[0].split(",")
    assert len(legends) == 31
    all_items = []
    codes = []
    legend_now = None
    items_now = []
    for line in lines[1:]:
        parts = line.split(",")
        if len(parts) == 1:
            if parts[0] == "EOF":
                break
            elif parts[0] == "E":
                if legend_now is not None and len(items_now) == 31:
                    all_items.append(items_now)
                    codes.append(legend_now)
                    items_now = []
                    legend_now = None
                else:
                    raise IOError("Unexpected E")
            else:
                raise IOError(f"Unexpected line: {parts[0]}")
        elif len(parts) == 7:
            if legend_now is None:
                legend_now = get_if_legend(parts[0])
                if legend_now is None:
                    raise IOError(f"Expected legend, got: {parts[0]}")
            else:
                if legend_now != parts[0][:6]:
                    raise IOError(f"Legend mismatch: {legend_now} vs {parts[0]}")
            open_, close, high, low, volume, amount = map(float, parts[1:])
            items_now.append(Item(open_, close, high, low, volume, amount))
        else:
            raise IOError(f"Unexpected line length: {len(parts)}")

    items = all_items
    code_to_index = {code: i for i, code in enumerate(codes)}
    date_to_index = {date: i for i, date in enumerate(legends)}
    print(f"Data loaded in {int((time.time() - start) * 1000)} ms.")
    print(f"Loaded {len(codes)} codes.")
    print(f"Dates: {legends}")
    example_code = codes[0]
    example_date = legends[0]
    code_index = code_to_index[example_code]
    date_index = date_to_index[example_date]
    example_item = items[code_index][date_index]
    print(f"Example item for code {example_code} on date {example_date}:")
    print(example_item)


if __name__ == "__main__":
    main()
