from fastapi import FastAPI, Request, Form
from fastapi.responses import HTMLResponse, PlainTextResponse

app = FastAPI()

@app.get("/report")
async def handler():
    return HTMLResponse("<html><body><h1>Duke's mood is: awake</h1></body></html>")


@app.get("/")
async def root():
    return PlainTextResponse("OK")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
