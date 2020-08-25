let roomCounts = 1;
let roomMaxCnt = 5;
let maxCnt = 20;
let minCnt = 1;
function increaseRoomCnt() {
    if (roomCounts < roomMaxCnt) {
        roomCounts += 1;
        document.getElementById("roomCnt").innerHTML = roomCounts;
        document.getElementById("adultCnt").innerHTML = parseInt(document.getElementById("adultCnt").innerHTML) + 1 ;

    }


};
function decreaseRoomCnt() {
    let adultCnt = parseInt(document.getElementById("adultCnt").innerHTML);
    let childCnt = parseInt(document.getElementById("childCnt").innerHTML);
    if (roomCounts > minCnt) {
        roomCounts -= 1;
        document.getElementById("roomCnt").innerHTML = roomCounts;
        if(adultCnt>7 && childCnt==0){
            document.getElementById("adultCnt").innerHTML =  parseInt(document.getElementById("adultCnt").innerHTML) - 4;
        }
        else if(childCnt>4){
          
            document.getElementById("childCnt").innerHTML =  parseInt(document.getElementById("childCnt").innerHTML) - 4;
            
        }
        else if(childCnt<4 && adultCnt!=0 && childCnt!=0){
        let total = 4-childCnt;
          alert(total);
            document.getElementById("childCnt").innerHTML =  parseInt(document.getElementById("childCnt").innerHTML) - childCnt;
            document.getElementById("adultCnt").innerHTML =  parseInt(document.getElementById("adultCnt").innerHTML) - parseInt(total);

        }
        else{
            document.getElementById("adultCnt").innerHTML =  roomCounts;
        }

       
        
    }
   
};

function increaseAdultCnt() {
    let roomCnt = document.getElementById("roomCnt").innerHTML; 
    let adultCnt = parseInt(document.getElementById("adultCnt").innerHTML);
    let childCnt = parseInt(document.getElementById("childCnt").innerHTML);
    let sum = roomCnt * 4 - childCnt;
console.log(adultCnt);
console.log(childCnt);
console.log(sum);
if(roomCnt<=5 && adultCnt<sum){
    document.getElementById("adultCnt").innerHTML =  parseInt(document.getElementById("adultCnt").innerHTML) + 1;
}



};
function decreaseAdultCnt() {
    let adultCnt = document.getElementById("adultCnt").innerHTML; 
   if (adultCnt > minCnt) {
        adultCnt -= 1;
        document.getElementById("adultCnt").innerHTML = adultCnt;
        document.getElementById("childCnt").innerHTML =  parseInt(document.getElementById("childCnt").innerHTML) + 1;

    }
};


function increaseChildCnt() {
    let roomCnt = document.getElementById("roomCnt").innerHTML; 
    let adultCnt = parseInt(document.getElementById("adultCnt").innerHTML);
    let childCnt = parseInt(document.getElementById("childCnt").innerHTML);
    let sum = roomCnt * 4 - adultCnt;
    
    if(roomCnt<=5 && childCnt<sum){
        document.getElementById("childCnt").innerHTML =  parseInt(document.getElementById("childCnt").innerHTML) + 1;
    }
};
function decreaseChildCnt() {
    let childCnt = document.getElementById("childCnt").innerHTML;
    if (childCnt > 0) {
        childCnt -= 1;
        document.getElementById("childCnt").innerHTML = childCnt;
        document.getElementById("adultCnt").innerHTML =  parseInt(document.getElementById("adultCnt").innerHTML) + 1;


    }
};