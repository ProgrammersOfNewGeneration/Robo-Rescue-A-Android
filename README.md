# Projeto de TCC grupo .PNG,the boys who are stick in the cat

# Protocolo de comunicação bluetoth Android <-> Arduino

Primeiro byte - PB - número da operação
  1 - escreve no pino
  2 - le um pino
  3 - chama rotina

Segundo byte - SB e Terceiro byte - TB
  se PB == 1:
    SB = numero do pino 
    TB = valor do pino
    
  se PB == 2:
    SB = numero do pino 
   
    O arduino devolverá o valor
    
  se PB == 3: 
    SB = numero da rotina
    TB = primeiro argumento da rotina
    
    O arduino devolverá o valor (ou os valores)
    se Android espera retorno:
    	-> O último parâmetro deve ser o ID do pedido
    	-> No retorno, o primeiro parâmetro é o ID do pedido, e o segundo o valor a ser enviado
    	
   
Usar '#' para informar final da mensagem
Usar '@' para delimitar parametro

# Valores do protocolo

## Número rotina
		1 - Motor.setVelocidade(byte v);
		2 - Motor::setVelocidadeMotor1(byte v);
		3 - Motor::setVelocidadeMotor2(byte v);
		4 - Motor::irFrente();
		5 - Motor::irDireita();
		6 - Motor::irDireitaForte();
		7 - Motor::irEsquerda();
		8 - Motor::irEsquerdaForte();
		9 - Motor::irRe();
		10 - Motor::irParar();
		11 - Ultrassom::setT_pin(int T_PIN);
		12 - Ultrassom::setE_pin(int E_PIN);
		13 - Ultrassom::setM_dist(int M_DIST);
		14 - Ultrassom::getT_pin();
		15 - Ultrassom::getE_pin();
		16 - Ultrassom::getM_dist();
		17 - Ultrassom::lerDistancia();

		18 - Arduino::delay(ms)
		19 - Robo::encruzilhada()
	

