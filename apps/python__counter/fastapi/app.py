from fastapi import FastAPI, Request, Form
from fastapi.responses import HTMLResponse, PlainTextResponse

app = FastAPI()

@app.get("/counter")
async def handler():
    app.state.counter = getattr(app.state, "counter", 0) + 1
    return PlainTextResponse(f"accessed {app.state.counter} time(s)")


@app.get("/")
async def root():
    return PlainTextResponse("OK")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
