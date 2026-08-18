// FULL-fidelity
use actix_web::{App, HttpServer};


async fn h_0() -> impl actix_web::Responder {
    actix_web::HttpResponse::Ok().json(serde_json::json!({"message": "Greetings!"}))
}

async fn h_1() -> impl actix_web::Responder {
    actix_web::HttpResponse::Ok().json(serde_json::json!({"message": "Greetings!"}))
}


#[actix_web::main]
async fn main() -> std::io::Result<()> {
    
    println!("Actix standalone on 8080");
    HttpServer::new(move || {
        App::new()
            
            .route("/standalone", actix_web::web::get().to(h_0))
            .route("/", actix_web::web::get().to(h_1))
    })
    .bind("0.0.0.0:8080")?
    .run()
    .await
}
