addKeydownListener = function(inputName){
    document.forms['queryForm'].elements[inputName].addEventListener("keydown", function(e) {
        if (!e) {
            alert("Javascript event listener not registered properly");
            return;
        }

        // Enter is pressed
        if (e.keyCode == 13) {
            if(document.forms['queryForm'].elements[inputName].value == ""){
                return;
            }
            document.forms['queryForm'].elements['search-type'].value = inputName;
            document.forms['queryForm'].submit();
        }
    }, false);
};

setActiveQueryAndSubmit = function(inputName){
    document.forms['queryForm'].elements['search-type'].value = inputName;
    document.forms['queryForm'].submit();
};

addKeydownListeners = function() {
    addKeydownListener('uuid');
    addKeydownListener('textquery');
    addKeydownListener('xpath');
    addKeydownListener('sql');
};
