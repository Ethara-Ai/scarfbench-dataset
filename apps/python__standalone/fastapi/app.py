"""FastAPI standalone service — modern async equivalent of Flask.
Business behavior: GET /standalone returns "Greetings!"
"""
from fastapi import FastAPI

app = FastAPI()


class StandaloneService:
    def return_message(self) -> str:
        return "Greetings!"


service = StandaloneService()


@app.get("/standalone")
async def greet():
    return {"message": service.return_message()}


@app.get("/")
async def root():
    return {"message": service.return_message()}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
