// FULL-fidelity
import { Controller, Get, Req, Res, Query } from '@nestjs/common';
import type { Response } from 'express';

@Controller()
export class AppController {
  private counterValue = 0;
  private readonly CIPHER_IN  = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private readonly CIPHER_OUT = "bcdefghijklmnopqrstuvwxyzaBCDEFGHIJKLMNOPQRSTUVWXYZA";
  private shift(s: string): string {
    let out = '';
    for (const ch of s) {
      const i = this.CIPHER_IN.indexOf(ch);
      out += i >= 0 ? this.CIPHER_OUT[i] : ch;
    }
    return out;
  }

  @Get('/standalone')
  handler0() { return {"message": "Greetings!"}; }

  @Get('/')
  handler1() { return {"message": "Greetings!"}; }
}
