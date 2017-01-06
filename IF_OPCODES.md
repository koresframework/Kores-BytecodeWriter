| Opcode        | Type          | Operand Stack | Branch if   |
| ------------- |:-------------:|:-------------:|:-----------:|
| IFEQ          | Int           | 1             | value == 0  |
| IFNE          | Int           | 1             | value != 0  |
| IFLT          | Int           | 1             | value <  0  |
| IFLE          | Int           | 1             | value <= 0  |
| IFGT          | Int           | 1             | value >  0  |
| IFGE          | Int           | 1             | value >= 0  |
| IF_ICMPEQ     | Int           | 2             | value 1 == value 2 |
| IF_ICMPNE     | Int           | 2             | value 1 != value 2 |
| IF_ICMPLT     | Int           | 2             | value 1 <  value 2 |
| IF_ICMPLE     | Int           | 2             | value 1 <= value 2 |
| IF_ICMPGT     | Int           | 2             | value 1 >  value 2 |
| IF_ICMPGE     | Int           | 2             | value 1 >= value 2 |
| IF_ACMPEQ     | Ref           | 2             | value 1 == value 2 |
| IF_ACMPNE     | Ref           | 2             | value 1 != value 2 |
| IFNONNULL     | Ref           | 1             | value != null      |
| IFNULL        | Ref           | 1             | value == null      |

*Ref* = Reference (`Object` instances)