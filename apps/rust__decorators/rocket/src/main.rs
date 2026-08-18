// FULL-fidelity
#[macro_use] extern crate rocket;

fn shift(s: &str) -> String {
    let cin: Vec<char> = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".chars().collect();
    let cout: Vec<char> = "bcdefghijklmnopqrstuvwxyzaBCDEFGHIJKLMNOPQRSTUVWXYZA".chars().collect();
    s.chars().map(|c| match cin.iter().position(|&x| x==c) { Some(i)=>cout[i], None=>c }).collect()
}


#[get("/decorators?<inputString>")]
async fn h_cipher_get_0(#[allow(non_snake_case)] inputString: Option<String>) -> (rocket::http::ContentType, String) {
    let s = inputString.unwrap_or_default();
    (rocket::http::ContentType::Plain, format!("Coded: {}", shift(&s)))
}

#[post("/decorators?<inputString>", data = "<form>")]
async fn h_cipher_post_1(#[allow(non_snake_case)] inputString: Option<String>, form: Option<rocket::form::Form<std::collections::HashMap<String,String>>>) -> (rocket::http::ContentType, String) {
    let s = form.as_ref().and_then(|f| f.get("inputString").cloned()).or(inputString).unwrap_or_default();
    (rocket::http::ContentType::Plain, format!("Coded: {}", shift(&s)))
}

#[get("/")]
async fn h_2() -> rocket::response::content::RawHtml<&'static str> {
    rocket::response::content::RawHtml("OK")
}


#[rocket::main]
async fn main() -> Result<(), rocket::Error> {
    let config = rocket::Config::figment()
        .merge(("address", "0.0.0.0"))
        .merge(("port", 8080u16));
    println!("Rocket decorators on 8080");
    rocket::custom(config)
        
        .mount("/", routes![h_cipher_get_0, h_cipher_post_1, h_2])
        .launch()
        .await?;
    Ok(())
}
