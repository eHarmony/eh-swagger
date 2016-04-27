$(function () {
    var hideHeader = window.location.search.match(/hideHeader=([^&]+)/);
    if (hideHeader) {
        document.getElementById("header").className = "hide-header";
    }

    $('#api_selector').hide();
    $('#logo').attr('href',"");
    $(document).prop('title', 'eHarmony Swagger UI');
});