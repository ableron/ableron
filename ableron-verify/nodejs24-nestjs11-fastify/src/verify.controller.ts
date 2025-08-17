import { Controller, Post, RawBodyRequest, Req, Res} from '@nestjs/common';
import { FastifyRequest, FastifyReply } from 'fastify';

@Controller()
export class VerifyController {
  @Post('/verify')
  verify(@Req() req: RawBodyRequest<FastifyRequest>, @Res() res: FastifyReply) {
    return res
      .header('Cache-Control', 'max-age=600')
      .header('Content-Type', 'text/html; charset=utf-8')
      .code(200)
      .send(req.rawBody?.toString() || '');
  }
}
