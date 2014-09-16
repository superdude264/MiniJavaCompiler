  0         JUMP         L11
  1  L10:   LOAD         -1[OB]
  2         RETURN (1)   0
  3  L11:   LOADL        -1
  4         LOADL        1
  5         LOADA        1[CB]
  6         JUMP         L13
  7  L12:   LOADL        1
  8         LOADL        -1
  9         LOADL        2
 10         CALL         newobj  
 11         LOAD         4[LB]
 12         LOADL        -1
 13         LOADL        2
 14         CALL         newobj  
 15         CALL         fieldupd
 16         LOAD         4[LB]
 17         CALL         fieldref
 18         LOAD         4[LB]
 19         CALL         fieldupd
 20         LOAD         4[LB]
 21         CALL         fieldref
 22         LOADL        7
 23         CALL         fieldupd
 24         LOAD         4[LB]
 25         CALL         fieldref
 26         CALL         fieldref
 27         LOADL        12
 28         CALL         fieldupd
 29         LOAD         4[LB]
 30         CALLI        L14
 31         RETURN (0)   1
 32  L13:   LOADL        -1
 33         LOADL        1
 34         LOADA        7[CB]
 35         JUMP         L15
 36  L14:   LOADA        0[OB]
 37         LOADL        1
 38         CALL         putintnl
 39         RETURN (0)   0
 40  L15:   LOADL        -1
 41         LOADL        1
 42         LOADA        36[CB]
 43         JUMP         L16
 44  L16:   LOADL        -1
 45         LOADL        0
 46         LOADL        0
 47         CALL         L12
 48         HALT   (0)   
