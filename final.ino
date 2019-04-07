#define echoPin1 2 // Echo Pin Sensor 1
#define trigPin1 3 // Trigger Pin Sensor 1

#define echoPin2 4 // Echo Pin Sensor 2
#define trigPin2 5 // Trigger Pin Sensor 2

#define echoPin3 6 // Echo Pin Sensor 3
#define trigPin3 7 // Trigger Pin Sensor 3

#define LEDPin 13

int maximumRange = 400; // Maximum range needed
int minimumRange = 0; // Minimum range needed

long duration1,duration2,duration3;
long distance1,distance2,distance3;

void setup() {
  Serial.begin (9600);
  pinMode(trigPin1, OUTPUT);
  pinMode(echoPin1, INPUT);
  pinMode(trigPin2, OUTPUT);
  pinMode(echoPin2, INPUT);
  pinMode(trigPin3, OUTPUT);
  pinMode(echoPin3, INPUT);
  pinMode(LEDPin, OUTPUT);  

}

void loop() {

  boolean left = false;
  boolean right = false;
  boolean front = false;

  // Checking for obstacles on left and right
  digitalWrite(trigPin1, LOW);
  delayMicroseconds(2);

  digitalWrite(trigPin2, LOW);
  delayMicroseconds(2);

  digitalWrite(trigPin3, LOW);
  delayMicroseconds(2);

  //=============================//
  digitalWrite(trigPin1, HIGH);
  delayMicroseconds(10);

  digitalWrite(trigPin2, HIGH);
  delayMicroseconds(10);

  digitalWrite(trigPin3, HIGH);
  delayMicroseconds(10);

  //=============================//
  digitalWrite(trigPin1, LOW);
  duration1 = pulseIn(echoPin1, HIGH);

  digitalWrite(trigPin2, LOW);
  duration2 = pulseIn(echoPin2, HIGH);

  digitalWrite(trigPin3, LOW);
  duration3 = pulseIn(echoPin3, HIGH);

  distance1 = duration1 / 58.2;
  distance2 = duration2 / 58.2;
  distance3 = duration3 / 58.2;

  //==============================================
  // Sensor1 obstacle on right
  if (distance1 >= maximumRange || distance1 <= minimumRange) {
    Serial.println("-1");
  } else {
    if (distance1 <= 100) {
      right = true;
    }
  }
  // Sensor2 obstacle on left
  if (distance2 >= maximumRange || distance2 <= minimumRange) {
    Serial.println("-1");
    digitalWrite(LEDPin, HIGH);
  } else {
    if (distance2 <= 100) {
      left = true;
    }
  }
  // Sensor3 obstacle on front
  if (distance3 >= maximumRange || distance3 <= minimumRange) {
    Serial.println("-1");
    digitalWrite(LEDPin, HIGH);
  } else {
    if (distance3 <= 100) {
      front = true;
    }
  }
  //==============================================
  if (left && right) {
    // Obstacle on left and right
    Serial.print("B");
  } else if (right) {
    // Obstacle on right
    Serial.print("R");
  }else if (front) {
    // Obstacle on front
    Serial.print("F");
  }
  else if (left) {
    // Obstacle on left
    Serial.print("L");
  }else if (right&&left&&front) {
    // Obstacle on right, left, front
    Serial.print("T");
  }else if (left&&front) {
    // Obstacle on left, front
    Serial.print("P");
  }else if (right&&front) {
    // Obstacle on right,front
    Serial.print("D");
  }
  else {
    // all clear
    Serial.print("C");
  }
 delay(1000);
}
