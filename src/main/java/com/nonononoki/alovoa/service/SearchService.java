package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.SearchDto;
import com.nonononoki.alovoa.model.SearchDto.SearchStage;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.model.UserSearchRequest;
import com.nonononoki.alovoa.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.recurse.geocoding.reverse.Country;
import uk.recurse.geocoding.reverse.ReverseGeocoder;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    public static final int SORT_DISTANCE = 1;
    public static final int SORT_ACTIVE_DATE = 2;
    public static final int SORT_INTEREST = 3;
    public static final int SORT_DONATION_LATEST = 4;
    public static final int SORT_DONATION_TOTAL = 5;
    public static final int SORT_NEWEST_USER = 6;
    private static final double LATITUDE = 111.1;
    private static final double LONGITUDE = 111.320;
    private static final int SEARCH_STEP_1 = 500;
    private static final int SEARCH_STEP_2 = 1000;
    private static final int DEFAULT_DISTANCE = 50;
    private static final int SEARCH_MAX = 200;
    private static ReverseGeocoder geocoder = null;
    @Autowired
    private TextEncryptorConverter textEncryptor;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private PublicService publicService;
    @Autowired
    private UserService userService;
    @Value("${app.search.max}")
    private int maxResults;
    @Value("${app.search.max.distance}")
    private int maxDistance;
    @Value("${app.age.min}")
    private int ageMin;
    @Value("${app.age.max}")
    private int ageMax;
    @Value("${app.search.estimate.max}")
    private int searchEstimateMax;

    public ReverseGeocoder getGeocoder() {
        if (geocoder == null) {
            geocoder = new ReverseGeocoder();
        }
        return geocoder;
    }

    public SearchDto searchDefault() throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            UnsupportedEncodingException {
        User user = authService.getCurrentUser(true);
        if (user.isAdmin()) {
            return SearchDto.builder().users(searchResultstoUserDto(userRepo.adminSearch(), 0, user)).build();
        }
        if (user.getLocationLatitude() != null) {
            return search(user.getLocationLatitude(), user.getLocationLongitude(), DEFAULT_DISTANCE,
                    SORT_DONATION_LATEST);
        } else {
            return null;
        }
    }

    public SearchDto search(Double latitude, Double longitude, int distance, int sortId) throws AlovoaException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {

        User user = authService.getCurrentUser(true);

        if (user.isAdmin()) {
            return SearchDto.builder().users(searchResultstoUserDto(userRepo.adminSearch(), 0, user)).build();
        }

        if (!latitude.equals(user.getLocationLatitude()) || !longitude.equals(user.getLocationLongitude()) || user.getCountry() == null) {
            Optional<String> country = getCountryIsoByLocation(latitude, longitude);
            if (country.isPresent()) {
                userService.updateCountry(country.get());
            }
        }

        Sort sort = switch (sortId) {
            case SORT_ACTIVE_DATE -> Sort.by(Sort.Direction.DESC, "dates.activeDate");
            case SORT_DONATION_LATEST -> Sort.by(Sort.Direction.DESC, "dates.latestDonationDate", "dates.creationDate");
            case SORT_DONATION_TOTAL -> Sort.by(Sort.Direction.DESC, "totalDonations");
            case SORT_NEWEST_USER -> Sort.by(Sort.Direction.DESC, "dates.creationDate");
            default -> Sort.unsorted();
        };

        int ageLegal = Tools.AGE_LEGAL;

        if (distance > maxDistance) {
            throw new AlovoaException("max_distance_exceeded");
        }

        user.getDates().setActiveDate(new Date());
        // rounding to improve privacy
        DecimalFormat df = new DecimalFormat("#.##");
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        user.setLocationLatitude(Double.valueOf(df.format(latitude)));
        user.setLocationLongitude(Double.valueOf(df.format(longitude)));
        userRepo.saveAndFlush(user);

        int age = Tools.calcUserAge(user);
        boolean isLegalAge = age >= ageLegal;
        int minAge = user.getPreferedMinAge();
        int maxAge = user.getPreferedMaxAge();

        if (isLegalAge && minAge < ageLegal) {
            minAge = ageLegal;
        }
        if (!isLegalAge && maxAge >= ageLegal) {
            maxAge = ageLegal - 1;
        }

        Date minDate = Tools.ageToDate(maxAge);
        Date maxDate = Tools.ageToDate(minAge);

        double deltaLat = distance / LATITUDE;
        double deltaLong = distance / (LONGITUDE * Math.cos(latitude / 180.0 * Math.PI));
        double minLat = latitude - deltaLat;
        double maxLat = latitude + deltaLat;
        double minLong = longitude - deltaLong;
        double maxLong = longitude + deltaLong;

        UserSearchRequest request = UserSearchRequest.builder().age(age).minLat(minLat).minLong(minLong).maxLat(maxLat)
                .maxLong(maxLong).maxDateDob(maxDate).minDateDob(minDate).intentionId(user.getIntention().getId())
                .likeIds(user.getLikes().stream().map(o -> o.getUserTo().getId()).collect(Collectors.toSet()))
                .blockIds(user.getBlockedUsers().stream().map(o -> o.getUserTo().getId()).collect(Collectors.toSet()))
                .hideIds(user.getHiddenUsers().stream().map(o -> o.getUserTo().getId()).collect(Collectors.toSet()))
                .genderIds(user.getPreferedGenders().stream().map(Gender::getId).collect(Collectors.toSet())).build();

        // because IS IN does not work with empty list
        request.getBlockIds().add(user.getId());
        request.getLikeIds().add(0L);
        request.getHideIds().add(0L);

        List<User> users = userRepo.usersSearch(request, PageRequest.of(0, SEARCH_MAX, sort));

        Set<Long> ignoreIds = user.getBlockedByUsers().stream().map(o -> o.getUserFrom().getId()).collect(Collectors.toSet());

        List<User> filteredUsers = filterUsers(users, ignoreIds, user, false);

        if (filteredUsers.size() < maxResults && users.size() >= searchEstimateMax) {
            List<User> allUsers = userRepo.usersSearch(request, PageRequest.of(0, Integer.MAX_VALUE, sort));
            if (allUsers.size() != users.size()) {
                filteredUsers = filterUsers(allUsers, ignoreIds, user, false);
            }
        }

        if (!filteredUsers.isEmpty()) {
            return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sortId, user))
                    .stage(SearchStage.NORMAL).build();
        }

        // NO COMPATIBLE USERS FOUND, SEARCH AROUND THE WORLD!

        distance = SEARCH_STEP_1;
        deltaLat = distance / LATITUDE;
        deltaLong = distance / (LONGITUDE * Math.cos(latitude / 180.0 * Math.PI));
        minLat = latitude - deltaLat;
        maxLat = latitude + deltaLat;
        minLong = longitude - deltaLong;
        maxLong = longitude + deltaLong;
        request.setMinLat(minLat);
        request.setMaxLat(maxLat);
        request.setMinLong(minLong);
        request.setMaxLong(maxLong);
        users = userRepo.usersSearch(request, PageRequest.of(0, SEARCH_MAX, sort));
        filteredUsers = filterUsers(users, ignoreIds, user, false);
        if (!filteredUsers.isEmpty()) {
            return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sortId, user))
                    .message(publicService.text("search.warning.global")).global(true)
                    .stage(SearchStage.INCREASED_RADIUS_1).build();
        }

        distance = SEARCH_STEP_2;
        deltaLat = distance / LATITUDE;
        deltaLong = distance / (LONGITUDE * Math.cos(latitude / 180.0 * Math.PI));
        minLat = latitude - deltaLat;
        maxLat = latitude + deltaLat;
        minLong = longitude - deltaLong;
        maxLong = longitude + deltaLong;
        request.setMinLat(minLat);
        request.setMaxLat(maxLat);
        request.setMinLong(minLong);
        request.setMaxLong(maxLong);
        users = userRepo.usersSearch(request, PageRequest.of(0, SEARCH_MAX, sort));
        filteredUsers = filterUsers(users, ignoreIds, user, false);
        if (!filteredUsers.isEmpty()) {
            return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sortId, user))
                    .message(publicService.text("search.warning.global")).global(true)
                    .stage(SearchStage.INCREASED_RADIUS_2).build();
        }

        users = userRepo.usersSearchAllIgnoreLocation(request, PageRequest.of(0, SEARCH_MAX, sort));
        filteredUsers = filterUsers(users, ignoreIds, user, false);

        if (!filteredUsers.isEmpty()) {
            return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sortId, user))
                    .message(publicService.text("search.warning.global")).global(true).stage(SearchStage.WORLD).build();
        }

        users = userRepo.usersSearchAllIgnoreLocationAndIntention(request, sort);
        filteredUsers = filterUsers(users, ignoreIds, user, false);
        if (!filteredUsers.isEmpty()) {
            return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sortId, user))
                    .message(publicService.text("search.warning.incompatible")).incompatible(true).global(true).stage(SearchStage.IGNORE_1)
                    .build();
        }

        if (isLegalAge) {
            maxDate = Tools.ageToDate(ageLegal);
            minDate = Tools.ageToDate(ageMax);
        } else {
            maxDate = Tools.ageToDate(ageMin);
            minDate = Tools.ageToDate(ageLegal - 1);
        }
        request.setMinDateDob(minDate);
        request.setMaxDateDob(maxDate);

        filteredUsers.clear();
        users = userRepo.usersSearchAllIgnoreLocationAndIntention(request, sort);
        filteredUsers = filterUsers(users, ignoreIds, user, false);
        if (!filteredUsers.isEmpty()) {
            return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sortId, user))
                    .message(publicService.text("search.warning.incompatible")).incompatible(true).global(true).stage(SearchStage.IGNORE_2)
                    .build();
        }

        users = userRepo.usersSearchAllIgnoreAll(request, sort);
        filteredUsers = filterUsers(users, ignoreIds, user, true);
        return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sortId, user))
                .message(publicService.text("search.warning.incompatible")).incompatible(true).global(true)
                .stage(SearchStage.IGNORE_ALL).build();

    }

    private List<User> filterUsers(List<User> users, Set<Long> ignoreIds, User user, boolean ignoreGenders) {
        List<User> filteredUsers = new ArrayList<>();
        for (User u : users) {
            if (ignoreIds.contains(u.getId())) {
                continue;
            }
            if (!ignoreGenders && !u.getPreferedGenders().contains(user.getGender())) {
                continue;
            }
            // square is fine, reduces CPU load when not calculating radius distance
            filteredUsers.add(u);

            if (filteredUsers.size() >= maxResults) {
                break;
            }
        }
        return filteredUsers;
    }

    private List<UserDto> searchResultstoUserDto(final List<User> userList, int sort, User user)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        List<UserDto> userDtos = new ArrayList<>();
        for (User u : userList) {
            UserDto dto = UserDto.userToUserDto(u, user, userService, textEncryptor, UserDto.PROFILE_PICTURE_ONLY);
            userDtos.add(dto);
        }

        if (sort == SORT_DISTANCE) {
            userDtos = userDtos.stream().sorted(Comparator.comparing(UserDto::getDistanceToUser))
                    .collect(Collectors.toList());
        } else if (sort == SORT_INTEREST) {
            Comparator<UserDto> comparatorCommonInterest = Comparator.comparing(f -> f.getCommonInterests().size());
            userDtos = userDtos.stream().filter(f -> !f.getCommonInterests().isEmpty())
                    .sorted(comparatorCommonInterest.reversed()
                            .thenComparing(Comparator.comparing(UserDto::getDistanceToUser).reversed()))
                    .collect(Collectors.toList());
        }

        return userDtos;
    }

    public Optional<String> getCountryIsoByLocation(double lat, double lon) {
        Optional<Country> country = getGeocoder().getCountry(lat, lon);
        return country.map(Country::iso);
    }

}
