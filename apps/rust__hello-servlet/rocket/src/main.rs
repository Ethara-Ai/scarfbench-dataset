// FULL-fidelity
#[macro_use] extern crate rocket;

fn shift(s: &str) -> String {
    let cin: Vec<char> = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".chars().collect();
    let cout: Vec<char> = "bcdefghijklmnopqrstuvwxyzaBCDEFGHIJKLMNOPQRSTUVWXYZA".chars().collect();
    s.chars().map(|c| match cin.iter().position(|&x| x==c) { Some(i)=>cout[i], None=>c }).collect()
}


#[get("/greeting?<name>")]
async fn h_greeting_0(name: Option<String>) -> rocket::response::content::RawHtml<String> {
    let n = name.unwrap_or_else(|| "World".to_string());
    rocket::response::content::RawHtml(format!("Hello, {}", n))
}

#[get("/")]
async fn h_1() -> rocket::response::content::RawHtml<&'static str> {
    rocket::response::content::RawHtml("Hello, World")
}


#[rocket::main]
async fn main() -> Result<(), rocket::Error> {
    let config = rocket::Config::figment()
        .merge(("address", "0.0.0.0"))
        .merge(("port", 8080u16));
    println!("Rocket hello-servlet on 8080");
    rocket::custom(config)
        
        .mount("/", routes![h_greeting_0, h_1])
        .launch()
        .await?;
    Ok(())
}
