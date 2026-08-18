from fastapi import FastAPI
import uvicorn

app = FastAPI()


def change(amount):
    res = []
    coins = [1, 5, 10, 25]
    coin_lookup = {25: "quarters", 10: "dimes", 5: "nickels", 1: "pennies"}
    coin = coins.pop()
    num, rem = divmod(int(amount * 100), coin)
    res.append({num: coin_lookup[coin]})
    while rem > 0:
        coin = coins.pop()
        num, rem = divmod(rem, coin)
        if num:
            if coin in coin_lookup:
                res.append({num: coin_lookup[coin]})
    return res


@app.get("/")
async def hello():
    return "Hello World! I can make change at route: /change"


@app.get("/change/{dollar}/{cents}")
async def changeroute(dollar: str, cents: str):
    amount = f"{dollar}.{cents}"
    return change(float(amount))


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
