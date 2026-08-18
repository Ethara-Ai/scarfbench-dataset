// FULL-fidelity
#[macro_use] extern crate rocket;


#[get("/standalone")]
async fn h_0() -> rocket::serde::json::Json<serde_json::Value> {
    rocket::serde::json::Json(serde_json::json!({"message": "Greetings!"}))
}

#[get("/")]
async fn h_1() -> rocket::serde::json::Json<serde_json::Value> {
    rocket::serde::json::Json(serde_json::json!({"message": "Greetings!"}))
}


#[rocket::main]
async fn main() -> Result<(), rocket::Error> {
    let config = rocket::Config::figment()
        .merge(("address", "0.0.0.0"))
        .merge(("port", 8080u16));
    println!("Rocket standalone on 8080");
    rocket::custom(config)
        
        .mount("/", routes![h_0, h_1])
        .launch()
        .await?;
    Ok(())
}
