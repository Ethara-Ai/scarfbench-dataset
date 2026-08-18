import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module.js';
import { NestExpressApplication } from '@nestjs/platform-express';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule, { logger: false });
  await app.listen(8080, '0.0.0.0');
  console.log(`NestJS standalone on 8080`);
}
bootstrap();
